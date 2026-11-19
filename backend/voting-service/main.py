import json
import os

import jwt
import pika
import psycopg2
from fastapi import FastAPI, Header, HTTPException
from psycopg2 import errors
from psycopg2.extras import RealDictCursor
from typing import List

from pydantic import BaseModel

JWT_SECRET = os.environ["JWT_SECRET"]

# Dos nodos de la misma base: la primaria escribe, la replica solo lee.
DB_PRIMARIA = os.environ["DB_PRIMARY_HOST"]
DB_REPLICA = os.environ["DB_REPLICA_HOST"]

app = FastAPI(title="Voting-Service")


def conectar(host):
    return psycopg2.connect(
        host=host,
        port=5432,
        user=os.environ["DB_USER"],
        password=os.environ["DB_PASSWORD"],
        dbname=os.environ["DB_NAME"],
    )


def consultar(host, sql, params=()):
    conn = conectar(host)
    try:
        with conn.cursor(cursor_factory=RealDictCursor) as cur:
            cur.execute(sql, params)
            return cur.fetchall()
    finally:
        conn.close()


def datos_token(authorization):
    """Valida el JWT que emitio el Auth-Service y devuelve su contenido."""
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Falta el token")
    token = authorization.split(" ", 1)[1]
    try:
        datos = jwt.decode(token, JWT_SECRET, algorithms=["HS256"])
        datos["sub"] = int(datos["sub"])
        return datos
    except (jwt.PyJWTError, KeyError, ValueError):
        raise HTTPException(status_code=401, detail="Token invalido o vencido")


def usuario_actual(authorization):
    """Devuelve el id del usuario del token."""
    return datos_token(authorization)["sub"]


def admin_actual(authorization):
    """Exige un token con rol ADMIN; devuelve el id del administrador."""
    datos = datos_token(authorization)
    if datos.get("rol") != "ADMIN":
        raise HTTPException(status_code=403, detail="Solo un administrador puede hacer esto")
    return datos["sub"]


def publicar_evento(evento):
    """Publica en RabbitMQ. Si falla, el voto ya esta guardado y no se pierde."""
    try:
        credenciales = pika.PlainCredentials(
            os.environ["RABBIT_USER"], os.environ["RABBIT_PASSWORD"]
        )
        conn = pika.BlockingConnection(
            pika.ConnectionParameters(host=os.environ["RABBIT_HOST"], credentials=credenciales)
        )
        canal = conn.channel()
        canal.exchange_declare(exchange="votos", exchange_type="fanout", durable=True)
        canal.basic_publish(
            exchange="votos",
            routing_key="",
            body=json.dumps(evento),
            properties=pika.BasicProperties(content_type="application/json", delivery_mode=2),
        )
        conn.close()
        return True
    except Exception as e:
        print(f"No se pudo publicar el evento: {e}", flush=True)
        return False


SQL_VOTACIONES = """
    SELECT v.id, v.titulo, v.descripcion,
           v.fecha_inicio AS "fechaInicio", v.fecha_fin AS "fechaFin", v.estado,
           COALESCE(
             json_agg(json_build_object('id', o.id, 'nombre', o.nombre) ORDER BY o.id)
             FILTER (WHERE o.id IS NOT NULL), '[]'
           ) AS opciones
    FROM votaciones v
    LEFT JOIN opciones o ON o.votacion_id = v.id
    {filtro}
    GROUP BY v.id
    ORDER BY v.id
"""


@app.get("/health")
def health():
    try:
        consultar(DB_PRIMARIA, "SELECT 1")
        consultar(DB_REPLICA, "SELECT 1")
    except psycopg2.Error as exc:
        print(f"Health check: base de datos no disponible: {exc}", flush=True)
        raise HTTPException(status_code=503, detail="Base de datos no disponible")
    return {"ok": True}


@app.get("/votaciones")
def listar():
    return consultar(DB_REPLICA, SQL_VOTACIONES.format(filtro=""))


@app.get("/votaciones/{votacion_id}")
def detalle(votacion_id: int):
    filas = consultar(DB_REPLICA, SQL_VOTACIONES.format(filtro="WHERE v.id = %s"), (votacion_id,))
    if not filas:
        raise HTTPException(status_code=404, detail="No se encontró la votación")
    return filas[0]


@app.get("/votaciones/{votacion_id}/resultados")
def resultados(votacion_id: int):
    filas = consultar(
        DB_REPLICA,
        """
        SELECT o.id, o.nombre, COUNT(v.id)::int AS votos
        FROM opciones o
        LEFT JOIN votos v ON v.opcion_id = o.id
        WHERE o.votacion_id = %s
        GROUP BY o.id, o.nombre
        ORDER BY o.id
        """,
        (votacion_id,),
    )
    if not filas:
        raise HTTPException(status_code=404, detail="No se encontró la votación")
    return {
        "votacionId": votacion_id,
        "total": sum(f["votos"] for f in filas),
        "opciones": filas,
    }


@app.get("/votaciones/{votacion_id}/mi-voto")
def mi_voto(votacion_id: int, authorization: str = Header(None)):
    usuario_id = usuario_actual(authorization)
    # Se lee de la PRIMARIA: la replica podria ir unos milisegundos atrasada.
    filas = consultar(
        DB_PRIMARIA,
        "SELECT 1 FROM votos WHERE votacion_id = %s AND usuario_id = %s",
        (votacion_id, usuario_id),
    )
    return {"yaVoto": len(filas) > 0}


class VotoIn(BaseModel):
    opcionId: int


@app.post("/votaciones/{votacion_id}/votos", status_code=201)
def votar(votacion_id: int, cuerpo: VotoIn, authorization: str = Header(None)):
    usuario_id = usuario_actual(authorization)

    estado = consultar(DB_PRIMARIA, "SELECT estado FROM votaciones WHERE id = %s", (votacion_id,))
    if not estado:
        raise HTTPException(status_code=404, detail="No se encontró la votación")
    if estado[0]["estado"] != "ACTIVA":
        raise HTTPException(status_code=409, detail="Esta votación no está abierta")

    conn = conectar(DB_PRIMARIA)
    try:
        with conn.cursor() as cur:
            cur.execute(
                "INSERT INTO votos (votacion_id, opcion_id, usuario_id) VALUES (%s, %s, %s)",
                (votacion_id, cuerpo.opcionId, usuario_id),
            )
        conn.commit()
    except errors.UniqueViolation:
        conn.rollback()
        # El UNIQUE (votacion_id, usuario_id) de la base garantiza el voto unico.
        raise HTTPException(status_code=409, detail="Ya has votado en esta votación")
    except errors.ForeignKeyViolation:
        conn.rollback()
        raise HTTPException(status_code=400, detail="Opción no válida")
    finally:
        conn.close()

    publicar_evento(
        {
            "evento": "VOTO_REGISTRADO",
            "votacionId": votacion_id,
            "usuarioId": usuario_id,
        }
    )
    return {"ok": True}


# ---------------------------------------------------------------------------
# Administracion (solo rol ADMIN). Las escrituras van a la PRIMARIA.
# ---------------------------------------------------------------------------

# Estados a los que se puede pasar desde cada estado.
TRANSICIONES = {
    "PROGRAMADA": {"ACTIVA", "CANCELADA"},
    "ACTIVA": {"FINALIZADA", "CANCELADA"},
    "FINALIZADA": set(),
    "CANCELADA": set(),
}


class VotacionIn(BaseModel):
    titulo: str
    descripcion: str
    duracionDias: int = 7
    abrirAhora: bool = True
    opciones: List[str]


class EstadoIn(BaseModel):
    estado: str


@app.post("/votaciones", status_code=201)
def crear_votacion(cuerpo: VotacionIn, authorization: str = Header(None)):
    admin_id = admin_actual(authorization)

    titulo = cuerpo.titulo.strip()
    descripcion = cuerpo.descripcion.strip()
    opciones = [o.strip() for o in cuerpo.opciones if o and o.strip()]

    if not 3 <= len(titulo) <= 150:
        raise HTTPException(status_code=400, detail="El título debe tener entre 3 y 150 caracteres")
    if len(descripcion) < 3:
        raise HTTPException(status_code=400, detail="Agrega una descripción")
    if not 1 <= cuerpo.duracionDias <= 90:
        raise HTTPException(status_code=400, detail="La duración debe estar entre 1 y 90 días")
    if len(opciones) < 2:
        raise HTTPException(status_code=400, detail="Agrega al menos dos opciones")
    if len(opciones) > 10:
        raise HTTPException(status_code=400, detail="Máximo 10 opciones")
    if any(len(o) > 150 for o in opciones):
        raise HTTPException(status_code=400, detail="Cada opción puede tener máximo 150 caracteres")
    if len({o.lower() for o in opciones}) != len(opciones):
        raise HTTPException(status_code=400, detail="Hay opciones repetidas")

    estado = "ACTIVA" if cuerpo.abrirAhora else "PROGRAMADA"

    conn = conectar(DB_PRIMARIA)
    try:
        with conn.cursor() as cur:
            # Una votacion programada empieza cuando el admin la abre; mientras tanto
            # se guarda con inicio en el futuro para que la regla fecha_fin > fecha_inicio se cumpla.
            cur.execute(
                """
                INSERT INTO votaciones (titulo, descripcion, fecha_inicio, fecha_fin, estado)
                VALUES (%s, %s,
                        CASE WHEN %s THEN now() ELSE now() + interval '1 day' END,
                        CASE WHEN %s THEN now() ELSE now() + interval '1 day' END
                          + make_interval(days => %s),
                        %s)
                RETURNING id
                """,
                (titulo, descripcion, cuerpo.abrirAhora, cuerpo.abrirAhora,
                 cuerpo.duracionDias, estado),
            )
            votacion_id = cur.fetchone()[0]
            for nombre in opciones:
                cur.execute(
                    "INSERT INTO opciones (votacion_id, nombre) VALUES (%s, %s)",
                    (votacion_id, nombre),
                )
        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()

    publicar_evento({"evento": "VOTACION_CREADA", "votacionId": votacion_id,
                     "estado": estado, "adminId": admin_id})

    # Se devuelve desde la PRIMARIA: la replica aun podria no tenerla.
    return consultar(DB_PRIMARIA, SQL_VOTACIONES.format(filtro="WHERE v.id = %s"), (votacion_id,))[0]


@app.patch("/votaciones/{votacion_id}/estado")
def cambiar_estado(votacion_id: int, cuerpo: EstadoIn, authorization: str = Header(None)):
    admin_id = admin_actual(authorization)
    nuevo = cuerpo.estado.strip().upper()
    if nuevo not in TRANSICIONES:
        raise HTTPException(status_code=400, detail="Estado no válido")

    conn = conectar(DB_PRIMARIA)
    try:
        with conn.cursor() as cur:
            # FOR UPDATE: dos admins cambiando el estado a la vez no se pisan.
            cur.execute("SELECT estado FROM votaciones WHERE id = %s FOR UPDATE", (votacion_id,))
            fila = cur.fetchone()
            if not fila:
                raise HTTPException(status_code=404, detail="No se encontró la votación")
            actual = fila[0]
            if nuevo not in TRANSICIONES[actual]:
                raise HTTPException(
                    status_code=409,
                    detail=f"No se puede pasar de {actual} a {nuevo}",
                )

            if nuevo == "ACTIVA":
                # Al abrir una programada empieza ahora y conserva su duracion.
                cur.execute(
                    """
                    UPDATE votaciones
                    SET fecha_fin = now() + (fecha_fin - fecha_inicio),
                        fecha_inicio = now(),
                        estado = 'ACTIVA'
                    WHERE id = %s
                    """,
                    (votacion_id,),
                )
            elif nuevo == "FINALIZADA":
                # Se cierra ya: la fecha de fin queda en este momento.
                cur.execute(
                    """
                    UPDATE votaciones
                    SET fecha_fin = GREATEST(fecha_inicio + interval '1 second', now()),
                        estado = 'FINALIZADA'
                    WHERE id = %s
                    """,
                    (votacion_id,),
                )
            else:
                cur.execute(
                    "UPDATE votaciones SET estado = %s WHERE id = %s", (nuevo, votacion_id)
                )
        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()

    publicar_evento({"evento": "VOTACION_" + nuevo, "votacionId": votacion_id, "adminId": admin_id})
    return consultar(DB_PRIMARIA, SQL_VOTACIONES.format(filtro="WHERE v.id = %s"), (votacion_id,))[0]
