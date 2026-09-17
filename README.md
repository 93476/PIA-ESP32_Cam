# PIA ESP32 + ESP32-CAM

Aplicación Android para visualizar en una sola interfaz dos dispositivos independientes:

- **ESP32 PIA/SIPROP**: sensores, presencia, distancia, temperatura, humedad, impacto y comandos GSM mediante HTTP.
- **ESP32-CAM**: video MJPEG en vivo mediante `/stream`.

La aplicación permite configurar la IP de cada dispositivo por separado. Para que ambos puedan verse al mismo tiempo, el teléfono y los dos ESP32 deben tener conectividad IP entre sí dentro de la misma red.

## Endpoints esperados

### ESP32 PIA
- `GET /api/status`
- `GET /api/cmd?accion=sms&mensaje=...`
- `GET /api/cmd?accion=llamada`
- `GET /api/cmd?accion=reset`
- `GET /api/cmd?accion=telefono&numero=...`

### ESP32-CAM
- `GET /stream`

## APK

El proyecto Android se encuentra en `android-app/`.

Cada push a `main` ejecuta GitHub Actions y genera un APK debug como artefacto descargable.
