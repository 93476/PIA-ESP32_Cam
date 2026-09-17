# PIA Unified APK Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Crear una APK Android en Kotlin que muestre estado y comandos del ESP32 PIA junto con el stream MJPEG de una ESP32-CAM independiente.

**Architecture:** La app usa un cliente HTTP pequeño para `/api/status` y `/api/cmd`, un modelo de estado parseado con `JSONObject`, y una `WebView` que muestra el endpoint `/stream`. Las URLs de ambos dispositivos se almacenan por separado en `SharedPreferences`.

**Tech Stack:** Android SDK 35, Kotlin 2.0.21, Android Gradle Plugin 8.7.3, Material Components, JUnit 4, GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-09-16-pia-unified-apk-design.md`

## Global Constraints

- Mantener los dos ESP32 independientes.
- No guardar credenciales WiFi ni números telefónicos privados en Git.
- Permitir HTTP cleartext porque los ESP32 no usan TLS.
- Base PIA inicial: `http://192.168.4.1`.
- Base CAM inicial: `http://192.168.4.2`.
- La UI debe seguir funcionando aunque uno de los dos dispositivos esté desconectado.

---

### Task 1: Android project + pure Kotlin URL/status core

**Files:**
- Create: `android-app/settings.gradle.kts`
- Create: `android-app/build.gradle.kts`
- Create: `android-app/app/build.gradle.kts`
- Create: `android-app/app/src/test/java/mx/conalep/pia/DeviceUrlsTest.kt`
- Create: `android-app/app/src/test/java/mx/conalep/pia/PiaStatusParserTest.kt`
- Create: `android-app/app/src/main/java/mx/conalep/pia/DeviceUrls.kt`
- Create: `android-app/app/src/main/java/mx/conalep/pia/PiaStatus.kt`

**Interfaces:**
- Produces: `DeviceUrls.normalizeBaseUrl(String): String`
- Produces: `DeviceUrls.statusUrl(String): String`
- Produces: `DeviceUrls.commandUrl(String, String, Map<String,String>): String`
- Produces: `PiaStatus.fromJson(String): PiaStatus`

- [ ] Write JUnit tests for URL normalization, command URL encoding, complete status JSON, and missing status fields.
- [ ] Run `gradle :app:testDebugUnitTest` and confirm tests fail because production classes are absent.
- [ ] Implement `DeviceUrls` and `PiaStatus` minimally.
- [ ] Re-run unit tests and confirm PASS.

### Task 2: HTTP client and preference-backed configuration

**Files:**
- Create: `android-app/app/src/main/java/mx/conalep/pia/PiaApiClient.kt`
- Create: `android-app/app/src/main/java/mx/conalep/pia/AppPreferences.kt`

**Interfaces:**
- Consumes: URL helpers and `PiaStatus` from Task 1.
- Produces: `PiaApiClient.fetchStatus(baseUrl): Result<PiaStatus>`.
- Produces: `PiaApiClient.sendCommand(baseUrl, action, params): Result<String>`.
- Produces: getters/setters for PIA URL, camera URL and phone.

- [ ] Implement blocking network calls intended to run on a background executor.
- [ ] Set connect/read timeouts and parse successful/non-successful HTTP responses.
- [ ] Persist only app configuration, not WiFi secrets.

### Task 3: Main Android UI and MJPEG camera view

**Files:**
- Create: `android-app/app/src/main/AndroidManifest.xml`
- Create: `android-app/app/src/main/res/layout/activity_main.xml`
- Create: `android-app/app/src/main/res/layout/dialog_settings.xml`
- Create: `android-app/app/src/main/res/values/strings.xml`
- Create: `android-app/app/src/main/res/values/colors.xml`
- Create: `android-app/app/src/main/res/values/themes.xml`
- Create: `android-app/app/src/main/res/xml/network_security_config.xml`
- Create: `android-app/app/src/main/java/mx/conalep/pia/MainActivity.kt`

**Interfaces:**
- Consumes: Task 1 and Task 2 classes.
- Produces: polling UI, command buttons, settings dialog, WebView stream loading.

- [ ] Build a scrollable Material UI with camera, connection indicators, sensor values, command controls and settings.
- [ ] Poll `/api/status` every 2 seconds while activity is visible.
- [ ] Render camera HTML containing `<img src="CAM_URL/stream">` in `WebView`.
- [ ] Add SMS, call, reset and camera reload actions.
- [ ] Add settings dialog and reload both clients after saving configuration.

### Task 4: CI build and APK artifact

**Files:**
- Create: `.github/workflows/android-apk.yml`
- Create: `.gitignore`

**Interfaces:**
- Produces: GitHub Actions artifact `PIA-Monitor-debug-apk` containing `app-debug.apk`.

- [ ] Configure JDK 17.
- [ ] Configure Gradle 8.7.
- [ ] Run `gradle -p android-app testDebugUnitTest assembleDebug --stacktrace`.
- [ ] Upload `android-app/app/build/outputs/apk/debug/app-debug.apk`.

### Task 5: Verification

- [ ] Confirm unit tests pass in GitHub Actions.
- [ ] Confirm debug APK assembles.
- [ ] Download the workflow artifact and provide the built APK to the user.
- [ ] Verify README points to the Android source and generated artifact workflow.
