const express = require('express');
const { Pool } = require('pg');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');

const JWT_SECRET = process.env.JWT_SECRET;
if (!JWT_SECRET) {
  console.error('Falta JWT_SECRET');
  process.exit(1);
}

const ROLES = ['CIUDADANO', 'ADMIN'];

// Conexión a la base auth (los datos vienen de las variables de entorno)
const pool = new Pool({
  host: process.env.DB_HOST,
  port: Number(process.env.DB_PORT || 5432),
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
});

const app = express();
app.use(express.json());

/* ------------------------------------------------------------------ */
/* Arranque: migración de la columna rol y administrador inicial       */
/* ------------------------------------------------------------------ */

const esperar = (ms) => new Promise((r) => setTimeout(r, ms));

async function migrar() {
  // Bases creadas antes de existir los roles no tienen la columna: se agrega aquí,
  // porque init.sql solo corre la primera vez que se crea el volumen.
  await pool.query(`
    ALTER TABLE usuarios
      ADD COLUMN IF NOT EXISTS rol VARCHAR(12) NOT NULL DEFAULT 'CIUDADANO'
  `);
  await pool.query(`
    DO $$ BEGIN
      IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'usuarios_rol_check') THEN
        ALTER TABLE usuarios ADD CONSTRAINT usuarios_rol_check CHECK (rol IN ('CIUDADANO','ADMIN'));
      END IF;
    END $$;
  `);
}

async function crearAdminInicial() {
  const documento = (process.env.ADMIN_DOCUMENTO || '').trim();
  const password = process.env.ADMIN_PASSWORD || '';
  const nombre = (process.env.ADMIN_NOMBRE || 'Administrador').trim();
  const correo = (process.env.ADMIN_CORREO || 'admin@votacion.local').trim();

  if (!documento || !password) {
    console.log('ADMIN_DOCUMENTO/ADMIN_PASSWORD no definidos: no se crea administrador inicial');
    return;
  }
  if (!/^\d{6,10}$/.test(documento) || password.length < 8) {
    console.error('ADMIN_DOCUMENTO debe tener 6 a 10 dígitos y ADMIN_PASSWORD al menos 8 caracteres');
    return;
  }

  // Si el documento ya existe, se convierte en ADMIN y su contraseña pasa a ser la del .env.
  const hash = await bcrypt.hash(password, 10);
  await pool.query(
    `INSERT INTO usuarios (nombre, documento, correo, password_hash, rol)
     VALUES ($1, $2, $3, $4, 'ADMIN')
     ON CONFLICT (documento) DO UPDATE
       SET rol = 'ADMIN', password_hash = EXCLUDED.password_hash`,
    [nombre, documento, correo, hash]
  );
  console.log(`Administrador inicial listo (documento ${documento})`);
}

async function iniciar() {
  for (let intento = 1; ; intento++) {
    try {
      await migrar();
      await crearAdminInicial();
      break;
    } catch (e) {
      if (intento >= 10) throw e;
      console.error(`Base no lista (intento ${intento}): ${e.message}`);
      await esperar(2000);
    }
  }
  app.listen(3000, '0.0.0.0', () => console.log('Auth-Service escuchando en el puerto 3000'));
}

/* ------------------------------------------------------------------ */
/* Autorización                                                        */
/* ------------------------------------------------------------------ */

function autenticado(req, res, next) {
  const h = req.headers.authorization || '';
  if (!h.startsWith('Bearer ')) return res.status(401).json({ error: 'Falta el token' });
  try {
    req.usuario = jwt.verify(h.slice(7), JWT_SECRET);
    next();
  } catch {
    res.status(401).json({ error: 'Token inválido o vencido' });
  }
}

function soloAdmin(req, res, next) {
  if (req.usuario?.rol !== 'ADMIN')
    return res.status(403).json({ error: 'Solo un administrador puede hacer esto' });
  next();
}

/* ------------------------------------------------------------------ */
/* Rutas                                                               */
/* ------------------------------------------------------------------ */

app.get('/health', async (_req, res) => {
  try {
    await pool.query('SELECT 1');
    res.json({ ok: true });
  } catch {
    res.status(503).json({ ok: false, error: 'Base de datos no disponible' });
  }
});

app.post('/auth/registro', async (req, res) => {
  const { nombre, documento, correo, password } = req.body || {};

  if (!nombre || nombre.trim().length < 3)
    return res.status(400).json({ error: 'Ingresa tu nombre completo' });
  if (!/^\d{6,10}$/.test(documento || ''))
    return res.status(400).json({ error: 'El documento debe tener entre 6 y 10 dígitos' });
  if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(correo || ''))
    return res.status(400).json({ error: 'Correo no válido' });
  if (!password || password.length < 8)
    return res.status(400).json({ error: 'Mínimo 8 caracteres' });

  try {
    const hash = await bcrypt.hash(password, 10);
    // El registro público siempre crea ciudadanos; el rol ADMIN solo lo asigna otro admin.
    await pool.query(
      `INSERT INTO usuarios (nombre, documento, correo, password_hash, rol)
       VALUES ($1, $2, $3, $4, 'CIUDADANO')`,
      [nombre.trim(), documento, correo.trim(), hash]
    );
    res.status(201).json({ ok: true });
  } catch (e) {
    if (e.code === '23505') // violación de UNIQUE en documento
      return res.status(409).json({ error: 'Este documento ya está registrado' });
    console.error(e);
    res.status(500).json({ error: 'Error interno' });
  }
});

app.post('/auth/login', async (req, res) => {
  const { documento, password } = req.body || {};
  try {
    const r = await pool.query(
      'SELECT id, nombre, documento, password_hash, rol FROM usuarios WHERE documento = $1',
      [documento]
    );
    const u = r.rows[0];
    const ok = u && (await bcrypt.compare(password || '', u.password_hash));
    if (!ok) return res.status(401).json({ error: 'Documento o contraseña incorrectos' });

    const token = jwt.sign(
      { sub: String(u.id), documento: u.documento, rol: u.rol },
      JWT_SECRET,
      { expiresIn: '2h' }
    );
    res.json({ token, nombre: u.nombre, rol: u.rol });
  } catch (e) {
    console.error(e);
    res.status(500).json({ error: 'Error interno' });
  }
});

// Lista de usuarios para el panel de administración (sin contraseñas).
app.get('/auth/usuarios', autenticado, soloAdmin, async (_req, res) => {
  try {
    const r = await pool.query(
      `SELECT id, nombre, documento, correo, rol, fecha_registro AS "fechaRegistro"
       FROM usuarios ORDER BY rol, nombre`
    );
    res.json(r.rows);
  } catch (e) {
    console.error(e);
    res.status(500).json({ error: 'Error interno' });
  }
});

// Cambiar el rol de un usuario. Un admin no puede quitarse el rol a sí mismo.
app.patch('/auth/usuarios/:id/rol', autenticado, soloAdmin, async (req, res) => {
  const id = Number(req.params.id);
  const { rol } = req.body || {};
  if (!Number.isInteger(id)) return res.status(400).json({ error: 'Usuario no válido' });
  if (!ROLES.includes(rol)) return res.status(400).json({ error: 'Rol no válido' });
  if (id === Number(req.usuario.sub) && rol !== 'ADMIN')
    return res.status(400).json({ error: 'No puedes quitarte el rol de administrador' });

  try {
    const r = await pool.query(
      `UPDATE usuarios SET rol = $1 WHERE id = $2
       RETURNING id, nombre, documento, correo, rol, fecha_registro AS "fechaRegistro"`,
      [rol, id]
    );
    if (!r.rows[0]) return res.status(404).json({ error: 'No se encontró el usuario' });
    res.json(r.rows[0]);
  } catch (e) {
    console.error(e);
    res.status(500).json({ error: 'Error interno' });
  }
});

iniciar().catch((e) => {
  console.error('No se pudo iniciar Auth-Service:', e);
  process.exit(1);
});
