# Sistema de Votación Biométrico

Aplicación Android con backend distribuido para desarrollo local. Docker Compose
levanta el gateway, los servicios de autenticación, votación y notificaciones,
tres bases PostgreSQL, una réplica de lectura y RabbitMQ.

## Estructura Docker

```text
backend/
├── .env.example                  Plantilla sin secretos; copia local ignorada por Git
├── docker-compose.yml            Servicios, red, healthchecks y volúmenes
├── docker.ps1                    Atajos para PowerShell en Windows
├── api-gateway/
│   ├── Dockerfile                Nginx sin root, puerto interno 8080
│   ├── .dockerignore             Excluye secretos y archivos innecesarios
│   └── nginx.conf                Rutas hacia los servicios por DNS de Compose
├── auth-service/
│   ├── Dockerfile                Node de producción y usuario no root
│   ├── .dockerignore
│   ├── package.json
│   ├── package-lock.json         Dependencias reproducibles
│   └── src/index.js
├── notification-service/         Estructura equivalente al servicio Node
├── voting-service/               FastAPI, requisitos fijados y usuario no root
└── db/                           Inicialización SQL de cada base y réplica
```

El gateway es el único servicio de aplicación publicado en el host. La app
Android del emulador usa `http://10.0.2.2:8081/`; los contenedores se conectan
entre sí con nombres como `auth-db`, `voting-db` y `rabbitmq`, nunca con
`localhost`.

## Requisitos

- Docker Desktop para Windows con el motor Linux iniciado y Compose v2.
- PowerShell 5.1 o superior.
- Python local solo para generar secretos si no está instalado, puedes crear
  secretos equivalentes con el gestor de contraseñas que prefieras.

## Preparar el entorno

Desde la raíz del repositorio, crea `backend/.env` solo si todavía no existe:

```powershell
if (-not (Test-Path backend\.env)) {
    Copy-Item backend\.env.example backend\.env
}
```

El nombre de proyecto predeterminado `backend` conserva los volúmenes del
Compose anterior (`backend_auth-data`, etc.). No lo cambies si quieres seguir
usando esos datos. Genera valores aleatorios y colócalos en las variables vacías de
`backend/.env`; no compartas ni subas ese archivo:

```powershell
python -c "import secrets; print('\n'.join(k+'='+secrets.token_urlsafe(32) for k in ['AUTH_DB_PASSWORD','VOTING_DB_PASSWORD','NOTIF_DB_PASSWORD','RABBIT_PASSWORD','REPL_PASSWORD','JWT_SECRET']))"
```

`RABBIT_USER` puede conservar el valor local de la plantilla. No reemplaces un
`.env` existente sin guardar sus valores. El Compose valida que las variables
requeridas estén definidas. El `.env` real está excluido de Git y de los
contextos de build.

## Arrancar y operar

Atajos desde PowerShell en la raíz:

```powershell
.\backend\docker.ps1 up
.\backend\docker.ps1 ps
.\backend\docker.ps1 logs
.\backend\docker.ps1 logs auth-service
.\backend\docker.ps1 shell auth-service
.\backend\docker.ps1 rebuild
.\backend\docker.ps1 down
```

También puedes usar Docker Compose directamente desde la raíz:

```powershell
docker compose --env-file backend/.env -f backend/docker-compose.yml config --quiet
docker compose --env-file backend/.env -f backend/docker-compose.yml up -d --build
docker compose --env-file backend/.env -f backend/docker-compose.yml ps
docker compose --env-file backend/.env -f backend/docker-compose.yml logs --tail=100
docker compose --env-file backend/.env -f backend/docker-compose.yml exec auth-service sh
docker compose --env-file backend/.env -f backend/docker-compose.yml down
```

`rebuild` reconstruye sin caché y vuelve a arrancar. Para solo reconstruir,
ejecuta `docker compose ... build --no-cache`. `down` conserva los datos; no
uses `down -v` salvo que quieras borrar intencionalmente todas las bases y la
cola persistentes.

## Validar que funciona

Desde la raíz, ejecuta en este orden:

```powershell
# 1. Sintaxis y variables necesarias
docker compose --env-file backend/.env -f backend/docker-compose.yml config --quiet

# 2. Reconstrucción completa, útil después de cambiar dependencias o Dockerfiles
docker compose --env-file backend/.env -f backend/docker-compose.yml build --no-cache

# 3. Arranque y estado; espera a que todos los servicios aparezcan healthy
docker compose --env-file backend/.env -f backend/docker-compose.yml up -d
docker compose --env-file backend/.env -f backend/docker-compose.yml ps

# 4. Logs recientes
docker compose --env-file backend/.env -f backend/docker-compose.yml logs --tail=100

# 5. Conectividad desde Windows al gateway y a la API de votaciones
curl.exe -fsS http://127.0.0.1:8081/health
curl.exe -fsS http://127.0.0.1:8081/votaciones

# 6. DNS entre contenedores y disponibilidad de la base de autenticación
docker compose --env-file backend/.env -f backend/docker-compose.yml exec auth-service node -e "require('dns').lookup('auth-db',(e,a)=>{if(e){console.error(e);process.exit(1)}console.log(a)})"
docker compose --env-file backend/.env -f backend/docker-compose.yml exec auth-db pg_isready -U auth_user -d auth
```

Para observar explícitamente los healthchecks:

```powershell
docker compose --env-file backend/.env -f backend/docker-compose.yml ps
docker compose --env-file backend/.env -f backend/docker-compose.yml ps -q | ForEach-Object {
    docker inspect --format '{{.Name}} {{if .State.Health}}{{.State.Health.Status}}{{else}}sin-healthcheck{{end}}' $_
}
```

### Persistencia y reinicio limpio

Comprueba persistencia primero con un `down` **sin** `-v`. Si quieres una
prueba verificable sin tocar datos de usuarios, crea una tabla temporal en la
base local, baja y sube el stack, confirma que sigue ahí y luego bórrala:

```powershell
docker compose --env-file backend/.env -f backend/docker-compose.yml exec auth-db psql -U auth_user -d auth -c "CREATE TABLE IF NOT EXISTS docker_persistence_check (id integer PRIMARY KEY); INSERT INTO docker_persistence_check VALUES (1) ON CONFLICT DO NOTHING;"
docker compose --env-file backend/.env -f backend/docker-compose.yml down
docker compose --env-file backend/.env -f backend/docker-compose.yml up -d
docker compose --env-file backend/.env -f backend/docker-compose.yml exec auth-db psql -U auth_user -d auth -c "SELECT * FROM docker_persistence_check;"
docker compose --env-file backend/.env -f backend/docker-compose.yml exec auth-db psql -U auth_user -d auth -c "DROP TABLE docker_persistence_check;"
```

Para un reinicio destructivo y bootstrap desde cero:

```powershell
docker compose --env-file backend/.env -f backend/docker-compose.yml down -v
docker compose --env-file backend/.env -f backend/docker-compose.yml up -d --build
```

`down -v` **elimina** `auth-data`, `voting-data`, `voting-replica-data`,
`notification-data` y `rabbit-data`; la persistencia no se puede comprobar
después de borrarlos. No ejecutes ese comando si necesitas conservar datos.
Si aún están activos los contenedores del Compose anterior, detén ese stack sin
`-v` antes de arrancar la versión nueva: ambos usan los mismos volúmenes y
puertos. No arranques dos PostgreSQL sobre el mismo volumen simultáneamente.

## Administrador

La app tiene dos roles: `CIUDADANO` (todos los que se registran) y `ADMIN`.

1. En `backend/.env` define el administrador inicial:
   ```
   ADMIN_DOCUMENTO=1000000001
   ADMIN_PASSWORD=una-clave-de-8-o-mas
   ```
2. Reinicia: `.\docker.ps1 up`. Al arrancar, Auth-Service agrega la columna `rol` si la base es antigua y crea ese usuario (o lo promueve a ADMIN si el documento ya existía).
3. Ingresa en la app con ese documento y contraseña. Aparece el botón **Administrar**.

Desde el panel el administrador puede crear votaciones (abiertas de inmediato o programadas), abrir, finalizar o cancelar votaciones y dar o quitar el rol de administrador a otros usuarios. El backend valida el rol dentro del token en cada petición: ocultar los botones en la app no es la protección.

| Método | Ruta (por el gateway) | Qué hace |
| --- | --- | --- |
| POST | `/votaciones` | Crea una votación con sus opciones |
| PATCH | `/votaciones/{id}/estado` | PROGRAMADA → ACTIVA/CANCELADA, ACTIVA → FINALIZADA/CANCELADA |
| GET | `/auth/usuarios` | Lista usuarios (sin contraseñas) |
| PATCH | `/auth/usuarios/{id}/rol` | Cambia el rol; un admin no puede quitárselo a sí mismo |

## Puertos, Android y desarrollo

| Host | Contenedor | Uso |
|---|---:|---|
| `127.0.0.1:8081` | `api-gateway:8080` | API para host y emulador Android |
| `127.0.0.1:15672` | `rabbitmq:15672` | Panel de RabbitMQ |
| Sin publicar | `auth-db:5432` | PostgreSQL interno |
| Sin publicar | `voting-db:5432` | PostgreSQL interno |
| Sin publicar | `notification-db:5432` | PostgreSQL interno |
| Sin publicar | `rabbitmq:5672` | AMQP interno |

Si un puerto está ocupado, cambia `GATEWAY_PORT` o `RABBITMQ_UI_PORT` en
`backend/.env`. Para usar un teléfono físico, establece `GATEWAY_HOST=0.0.0.0`,
permite el puerto en el firewall y cambia las dos URL de `ApiConfig.kt` a la IP
LAN del PC. No uses `127.0.0.1` como destino desde el teléfono.

La configuración actual es para desarrollo local; no se añade un override de
producción. Para producción hacen falta, como mínimo, TLS, secretos gestionados
por la plataforma, límites de recursos ajustados al host y una política de
exposición de red distinta.

## Problemas comunes

| Si ves este error / síntoma | Causa probable | Solución aplicada |
|---|---|---|
| `port is already allocated` | Puerto host ocupado | Cambia `GATEWAY_PORT` o `RABBITMQ_UI_PORT`; los PostgreSQL no se publican al host. |
| Android muestra “No se pudo conectar” | `127.0.0.1` apunta al emulador/teléfono | El emulador usa `10.0.2.2:8081`; el teléfono usa la IP LAN del PC. |
| `connection refused` al iniciar | Dependencia aún no lista o servicio sin `0.0.0.0` | `depends_on: service_healthy`, healthchecks y bind explícito en Node/FastAPI. |
| La réplica no inicia / `password authentication failed` | Credencial de réplica distinta en DB y `.env` | El rol de réplica obtiene su contraseña de `REPL_PASSWORD`; reinicializa solo si el volumen es nuevo o actualiza el rol si ya existe. |
| Se perdieron datos | Se ejecutó `down -v` o no había volumen | Las bases y RabbitMQ usan volúmenes nombrados; usa `down` sin `-v` para conservarlos. |
| `permission denied` en datos | UID/GID del volumen incompatible | PostgreSQL corre con su usuario oficial; apps Node usan `node` y FastAPI UID 10001; no montes carpetas de host sobre los datos. |
| `bad interpreter` o `$'\r'` | Script `.sh` guardado con CRLF | `.gitattributes` fija LF para `.sh`; vuelve a guardar `pg_hba_repl.sh` como LF si el editor lo cambió. |
| Build copia `node_modules`, `.env` o `.git` | Falta/falla `.dockerignore` | Cada contexto de servicio tiene `.dockerignore`; los secretos también se excluyen del build. |
| Build reinstala o descarga paquetes en cada cambio de código | `COPY` del código antes de los manifiestos | Los Dockerfiles copian primero `package*.json`/`requirements.txt` y luego el código. |
| `npm ci` falla por lockfile | Lockfile ausente o desincronizado | Node usa `package-lock.json`; sincronízalo con el manifiesto y confirma el archivo en Git. `requirements.txt` fija versiones directas y transitivas de Python. |
| La app no acepta una contraseña nueva tras editar `.env` | PostgreSQL/RabbitMQ inicializan credenciales solo al crear sus volúmenes | Actualiza la contraseña dentro del servicio o recrea intencionalmente sus volúmenes; cambiar `.env` no modifica datos ya inicializados. |
| Falta `JWT_SECRET` u otra variable | `.env` no existe, está incompleto o no se cargó | Usa `--env-file backend/.env`; no se usa el `.env.example` automáticamente. |
| Funciona local pero no fuera de tu PC | Configuración local no es producción | Esta configuración no pretende ser producción; prepara TLS, secretos, firewall y límites antes de desplegar. |
| Imagen pesada / herramientas de compilación innecesarias | Dependencias de desarrollo o build incluidas | Node instala solo producción en etapa separada; Python usa slim y no instala toolchain. |
| Fechas o caracteres incorrectos | Zona horaria/encoding inconsistente | Servicios usan UTC; PostgreSQL inicializa en UTF-8 y FastAPI declara locale UTF-8. |
| Conflicto de nombres de contenedor | Nombre global fijo compartido | No se fija `container_name`; Compose namespacia servicios, red y volúmenes por proyecto. |
| `ENOTFOUND auth-db` / conexión entre servicios falla | Usar `localhost` dentro de un contenedor o red distinta | Todos comparten la red `app`; usan nombres de servicio y puertos internos. |

## Checklist final

- [ ] `backend/.env` tiene secretos aleatorios y está ignorado por Git.
- [ ] `docker compose config --quiet` y `build --no-cache` terminan correctamente.
- [ ] Todos los servicios importantes muestran estado `healthy`.
- [ ] El gateway responde en el puerto esperado y Android apunta a la dirección correcta.
- [ ] La prueba con `down` (sin `-v`) confirma que los datos persisten.
- [ ] Entiendo que `down -v` destruye los volúmenes locales.
- [ ] Imágenes y dependencias están fijadas; los lockfiles están versionados.
- [ ] No se exponen bases de datos ni RabbitMQ fuera del host por defecto.
- [ ] Los logs van a stdout/stderr con rotación.
- [ ] Hay una configuración separada y revisada antes de cualquier despliegue productivo.
