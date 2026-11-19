const express = require('express');
const { Pool } = require('pg');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');

const JWT_SECRET = process.env.JWT_SECRET;
if (!JWT_SECRET) {
  console.error('Falta JWT_SECRET');
  process.exit(1);
}

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
    await pool.query(
      'INSERT INTO usuarios (nombre, documento, correo, password_hash) VALUES ($1, $2, $3, $4)',
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
      'SELECT id, nombre, documento, password_hash FROM usuarios WHERE documento = $1',
      [documento]
    );
    const u = r.rows[0];
    const ok = u && (await bcrypt.compare(password || '', u.password_hash));
    if (!ok) return res.status(401).json({ error: 'Documento o contraseña incorrectos' });

    const token = jwt.sign({ sub: u.id, documento: u.documento }, JWT_SECRET, { expiresIn: '2h' });
    res.json({ token, nombre: u.nombre });
  } catch (e) {
    console.error(e);
    res.status(500).json({ error: 'Error interno' });
  }
});

app.listen(3000, '0.0.0.0', () => console.log('Auth-Service escuchando en el puerto 3000'));