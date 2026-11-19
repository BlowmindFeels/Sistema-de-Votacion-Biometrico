CREATE TABLE usuarios (
    id             SERIAL PRIMARY KEY,
    nombre         VARCHAR(120) NOT NULL,
    documento      VARCHAR(10)  NOT NULL UNIQUE,
    correo         VARCHAR(150) NOT NULL,
    password_hash  VARCHAR(100) NOT NULL,
    rol            VARCHAR(12)  NOT NULL DEFAULT 'CIUDADANO',
    fecha_registro TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT usuarios_rol_check CHECK (rol IN ('CIUDADANO','ADMIN'))
);
