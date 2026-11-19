@echo off
set BASE_GATEWAY=http://localhost:8081

echo ==============================================
echo VERIFICACION DE SERVICIOS
echo ==============================================
curl -s %BASE_GATEWAY%/health
echo.
curl -s %BASE_GATEWAY%/votaciones
echo.
echo ==============================================
