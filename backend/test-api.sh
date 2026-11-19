#!/bin/bash
# Script de pruebas automatizadas con curl para el backend distribuido de votación

BASE_AUTH="http://localhost:3001"
BASE_VOTING="http://localhost:3002"
BASE_NOTIF="http://localhost:3003"
BASE_GATEWAY="http://localhost:8081"

DOC_TEST=$((1000000000 + RANDOM % 8999999999))
EMAIL_TEST="usuario_${DOC_TEST}@correo.com"
PASS_TEST="ClaveSegura123"

echo "=============================================="
echo "1. VERIFICACIÓN DE SALUD (HEALTH CHECKS)"
echo "=============================================="
echo -n "Auth-Service: "
curl -s "${BASE_AUTH}/health"
echo ""

echo -n "Voting-Service: "
curl -s "${BASE_VOTING}/health"
echo ""

echo -n "Notification-Service: "
curl -s "${BASE_NOTIF}/health"
echo ""

echo -n "API Gateway: "
curl -s "${BASE_GATEWAY}/health"
echo ""
echo ""

echo "=============================================="
echo "2. REGISTRO DE USUARIO DE PRUEBA"
echo "=============================================="
REG_RESP=$(curl -s -X POST "${BASE_GATEWAY}/auth/registro" \
  -H "Content-Type: application/json" \
  -d "{\"nombre\":\"Usuario Test Auto\",\"documento\":\"${DOC_TEST}\",\"correo\":\"${EMAIL_TEST}\",\"password\":\"${PASS_TEST}\"}")
echo "Respuesta Registro: ${REG_RESP}"
echo ""

echo "=============================================="
echo "3. INICIO DE SESIÓN (LOGIN JWT)"
echo "=============================================="
LOGIN_RESP=$(curl -s -X POST "${BASE_GATEWAY}/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"documento\":\"${DOC_TEST}\",\"password\":\"${PASS_TEST}\"}")
echo "Respuesta Login: ${LOGIN_RESP}"

TOKEN=$(echo "${LOGIN_RESP}" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
echo "JWT Token extraído: ${TOKEN}"
echo ""

if [ -z "$TOKEN" ]; then
  echo "ERROR: No se pudo obtener el token JWT."
  exit 1
fi

echo "=============================================="
echo "4. LISTAR Y OBTENER DETALLE DE VOTACIONES"
echo "=============================================="
echo "Lista de Votaciones (Réplica):"
curl -s "${BASE_GATEWAY}/votaciones"
echo ""
echo ""
echo "Detalle Votación #1 (Réplica):"
curl -s "${BASE_GATEWAY}/votaciones/1"
echo ""
echo ""

echo "=============================================="
echo "5. REGISTRO DE VOTO CON HUELA / JWT"
echo "=============================================="
VOTO_RESP=$(curl -s -X POST "${BASE_GATEWAY}/votaciones/1/votos" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d "{\"opcionId\":1}")
echo "Respuesta Voto #1: ${VOTO_RESP}"
echo ""

echo "Intento de Voto Duplicado (Debe responder 409):"
curl -s -X POST "${BASE_GATEWAY}/votaciones/1/votos" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d "{\"opcionId\":2}"
echo ""
echo ""

echo "=============================================="
echo "6. RESULTADOS DE LA VOTACIÓN"
echo "=============================================="
curl -s "${BASE_GATEWAY}/votaciones/1/resultados"
echo ""
echo ""

echo "=============================================="
echo "7. NOTIFICACIONES RECIBIDAS (RABBITMQ CONSUMER)"
echo "=============================================="
# Esperar 1 segundo para procesamiento asíncrono de RabbitMQ
sleep 1
curl -s "${BASE_GATEWAY}/notificaciones" \
  -H "Authorization: Bearer ${TOKEN}"
echo ""
echo "=============================================="
echo "PRUEBAS FINALIZADAS EXITOSAMENTE."
