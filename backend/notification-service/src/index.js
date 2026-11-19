const express = require('express');
const { Pool } = require('pg');
const amqp = require('amqplib');
const jwt = require('jsonwebtoken');

const JWT_SECRET = process.env.JWT_SECRET;
if (!JWT_SECRET) {
  console.error('Falta JWT_SECRET');
  process.exit(1);
}

const pool = new Pool({
  host: process.env.DB_HOST,
  port: Number(process.env.DB_PORT || 5432),
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
});

const app = express();
app.use(express.json());

let rabbitConectado = false;

// Middleware para validar token JWT
function authMiddleware(req, res, next) {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'Falta el token' });
  }
  const token = authHeader.split(' ')[1];
  try {
    const decoded = jwt.verify(token, JWT_SECRET);
    req.usuarioId = Number(decoded.sub);
    next();
  } catch (e) {
    res.status(401).json({ error: 'Token inválido o vencido' });
  }
}

app.get('/health', async (_req, res) => {
  try {
    await pool.query('SELECT 1');
    if (!rabbitConectado) {
      return res.status(503).json({ ok: false, error: 'RabbitMQ no disponible' });
    }
    res.json({ ok: true });
  } catch {
    res.status(503).json({ ok: false, error: 'Base de datos no disponible' });
  }
});

// Registrar token FCM de dispositivo
app.post('/dispositivos', authMiddleware, async (req, res) => {
  const { tokenFcm } = req.body || {};
  if (!tokenFcm || typeof tokenFcm !== 'string') {
    return res.status(400).json({ error: 'Falta tokenFcm' });
  }
  try {
    await pool.query(
      `INSERT INTO dispositivos (usuario_id, token_fcm)
       VALUES ($1, $2)
       ON CONFLICT (token_fcm) DO UPDATE SET usuario_id = EXCLUDED.usuario_id, fecha = now()`,
      [req.usuarioId, tokenFcm]
    );
    res.status(201).json({ ok: true });
  } catch (e) {
    console.error(e);
    res.status(500).json({ error: 'Error al registrar dispositivo' });
  }
});

// Consultar notificaciones del usuario
app.get('/notificaciones', authMiddleware, async (req, res) => {
  try {
    const r = await pool.query(
      'SELECT id, mensaje, enviada, fecha FROM notificaciones WHERE usuario_id = $1 ORDER BY fecha DESC LIMIT 50',
      [req.usuarioId]
    );
    res.json(r.rows);
  } catch (e) {
    console.error(e);
    res.status(500).json({ error: 'Error al obtener notificaciones' });
  }
});

// Consumir eventos de RabbitMQ (Exchange: votos)
async function conectarRabbitMQ() {
  const rabbitUser = process.env.RABBIT_USER;
  const rabbitPass = process.env.RABBIT_PASSWORD;
  const rabbitHost = process.env.RABBIT_HOST || 'rabbitmq';
  const url = `amqp://${rabbitUser}:${rabbitPass}@${rabbitHost}:5672`;

  while (true) {
    try {
      console.log('Conectando a RabbitMQ...');
      const conn = await amqp.connect(url);
      const channel = await conn.createChannel();
      const exchange = 'votos';

      await channel.assertExchange(exchange, 'fanout', { durable: true });
      const q = await channel.assertQueue('notificaciones_votos', { durable: true });
      await channel.bindQueue(q.queue, exchange, '');
      rabbitConectado = true;

      console.log('Escuchando eventos de votos en RabbitMQ...');
      channel.consume(q.queue, async (msg) => {
        if (!msg) return;
        try {
          const contenido = JSON.parse(msg.content.toString());
          console.log('Evento recibido:', contenido);

          if (contenido.evento === 'VOTO_REGISTRADO') {
            const mensaje = `Tu voto para la votación #${contenido.votacionId} fue registrado con éxito.`;
            await pool.query(
              'INSERT INTO notificaciones (usuario_id, mensaje, enviada) VALUES ($1, $2, true)',
              [contenido.usuarioId, mensaje]
            );
          }
          channel.ack(msg);
        } catch (err) {
          console.error('Error procesando evento:', err);
          channel.nack(msg, false, false);
        }
      });

      conn.on('error', (err) => {
        rabbitConectado = false;
        console.error('Error en conexión RabbitMQ:', err);
      });
      conn.on('close', () => {
        rabbitConectado = false;
        console.warn('Conexión a RabbitMQ cerrada. Reintentando en 5s...');
        setTimeout(conectarRabbitMQ, 5000);
      });
      break;
    } catch (err) {
      console.error('No se pudo conectar a RabbitMQ. Reintentando en 5s...', err.message);
      await new Promise((r) => setTimeout(r, 5000));
    }
  }
}

app.listen(3000, '0.0.0.0', () => {
  console.log('Notification-Service escuchando en puerto 3000');
  conectarRabbitMQ();
});
