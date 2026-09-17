# PIA Unified Android App Design

## Objetivo

Crear una APK Android que muestre en una sola aplicación el estado del ESP32 principal PIA/SIPROP y el video en vivo de una ESP32-CAM, manteniendo ambos microcontroladores y sus firmwares independientes.

## Arquitectura

La APK funciona como cliente de dos servicios HTTP separados:

1. **ESP32 PIA/SIPROP**
   - Provee estado por `GET /api/status`.
   - Ejecuta acciones por `GET /api/cmd`.
   - La respuesta de estado contiene `wifi`, `ssid`, `ip`, `apSsid`, `apIp`, `telefonoConfigurado`, `estado`, `presencia`, `fueraRango`, `distancia`, `temperatura`, `humedad`, `impacto`, `smsEnviado`, `llamadaRealizada`, `segundosPresencia` y `llamadaEn`.

2. **ESP32-CAM**
   - Provee el stream MJPEG por `GET /stream`.
   - La APK lo presenta dentro de una vista WebView dedicada.

La APK no mezcla los firmwares ni hace que un ESP32 dependa lógicamente del otro. Solo requiere que el teléfono pueda alcanzar por IP a ambos dispositivos.

## Tecnología

- Android nativo.
- Kotlin.
- Min SDK 24.
- Target/compile SDK 35.
- Android Gradle Plugin 8.7.3.
- Kotlin Android plugin 2.0.21.
- UI clásica con XML y Material Components.
- `HttpURLConnection` + `org.json.JSONObject` para no introducir una dependencia de red adicional.
- `WebView` para visualizar MJPEG.
- `SharedPreferences` para persistir las IP configuradas.

## Pantalla principal

La pantalla muestra:

- Título PIA Monitor.
- Indicador de conexión del PIA.
- Indicador de cámara.
- Panel de cámara en vivo.
- Estado general.
- Presencia.
- Distancia.
- Temperatura.
- Humedad.
- Impacto.
- Estado SMS.
- Estado de llamada.
- Tiempo de presencia.
- Tiempo restante para llamada automática.
- Botón Enviar SMS.
- Botón Llamar.
- Botón Reiniciar alertas.
- Botón Configuración.

## Configuración

La configuración permite editar y guardar:

- Base URL del PIA. Valor inicial: `http://192.168.4.1`.
- Base URL de la cámara. Valor inicial: `http://192.168.4.2`.
- Número telefónico opcional para enviarlo al ESP32 mediante la acción `telefono`.

Las URLs se normalizan eliminando `/` finales para construir rutas sin duplicaciones.

## Flujo de datos

1. Al iniciar la app se cargan las URLs guardadas.
2. La app consulta `PIA_BASE_URL/api/status` aproximadamente cada 2 segundos mientras la pantalla está visible.
3. El JSON se convierte a un modelo `PiaStatus`.
4. La UI se actualiza en el hilo principal.
5. La cámara carga una pequeña página HTML local cuyo `<img>` apunta a `CAM_BASE_URL/stream`.
6. Los botones invocan `PIA_BASE_URL/api/cmd` con parámetros URL-encoded.
7. El usuario puede recargar la cámara desde la interfaz si pierde conexión.

## Red y seguridad local

Los ESP32 actuales usan HTTP sin TLS, por lo que la APK habilita cleartext HTTP de forma explícita. No se incluyen credenciales WiFi ni números telefónicos privados dentro del repositorio Android. La app guarda únicamente los valores que el usuario capture en el almacenamiento privado de la aplicación.

## Manejo de errores

- Si `/api/status` no responde, la app muestra PIA desconectado y conserva la interfaz operativa.
- Si el JSON está incompleto, se usan valores seguros por defecto.
- Si un comando HTTP falla, se muestra un mensaje breve con el error.
- Si la cámara no carga, la app muestra una indicación de desconexión y un botón para recargar.
- Las consultas se realizan fuera del hilo principal.

## Pruebas

Las pruebas unitarias cubren:

- Parseo del JSON de `/api/status`.
- Valores por defecto ante campos ausentes.
- Normalización de URLs.
- Construcción de URLs de comandos y URL-encoding.

La compilación CI ejecuta `testDebugUnitTest` y `assembleDebug` y publica el APK debug como artefacto.

## Criterios de aceptación

- El proyecto Android compila desde un checkout limpio mediante Gradle.
- La app permite usar IPs diferentes para PIA y ESP32-CAM.
- El estado de PIA se actualiza automáticamente.
- El stream MJPEG aparece en la misma aplicación.
- SMS, llamada y reset pueden invocarse desde la app.
- Los datos de conexión sobreviven al cierre y reapertura de la app.
- GitHub Actions produce un APK debug descargable.
