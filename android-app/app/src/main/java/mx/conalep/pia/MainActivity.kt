package mx.conalep.pia

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {

    private lateinit var preferences: AppPreferences
    private val apiClient = PiaApiClient()
    private val executor = Executors.newFixedThreadPool(3)
    private val handler = Handler(Looper.getMainLooper())
    private val statusRequestInFlight = AtomicBoolean(false)

    private lateinit var btnTabMonitor: MaterialButton
    private lateinit var btnTabCamera: MaterialButton
    private lateinit var monitorContainer: android.view.View
    private lateinit var cameraContainer: android.view.View
    private lateinit var tvPiaConnection: TextView
    private lateinit var tvCameraConnection: TextView
    private lateinit var webCamera: WebView
    private lateinit var tvGeneralState: TextView
    private lateinit var tvPresence: TextView
    private lateinit var tvDistance: TextView
    private lateinit var tvTemperature: TextView
    private lateinit var tvHumidity: TextView
    private lateinit var tvImpact: TextView
    private lateinit var tvSms: TextView
    private lateinit var tvCall: TextView
    private lateinit var tvPresenceTime: TextView
    private lateinit var tvCallCountdown: TextView
    private lateinit var tvNetworkInfo: TextView

    private val statusPoller = object : Runnable {
        override fun run() {
            requestStatus()
            handler.postDelayed(this, STATUS_INTERVAL_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        preferences = AppPreferences(this)
        bindViews()
        configureButtons()
        configureCameraWebView()
        showTab(camera = false)
    }

    override fun onStart() {
        super.onStart()
        handler.removeCallbacks(statusPoller)
        handler.post(statusPoller)
    }

    override fun onStop() {
        handler.removeCallbacks(statusPoller)
        super.onStop()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        webCamera.loadUrl("about:blank")
        webCamera.destroy()
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun bindViews() {
        btnTabMonitor = findViewById(R.id.btnTabMonitor)
        btnTabCamera = findViewById(R.id.btnTabCamera)
        monitorContainer = findViewById(R.id.monitorContainer)
        cameraContainer = findViewById(R.id.cameraContainer)
        tvPiaConnection = findViewById(R.id.tvPiaConnection)
        tvCameraConnection = findViewById(R.id.tvCameraConnection)
        webCamera = findViewById(R.id.webCamera)
        tvGeneralState = findViewById(R.id.tvGeneralState)
        tvPresence = findViewById(R.id.tvPresence)
        tvDistance = findViewById(R.id.tvDistance)
        tvTemperature = findViewById(R.id.tvTemperature)
        tvHumidity = findViewById(R.id.tvHumidity)
        tvImpact = findViewById(R.id.tvImpact)
        tvSms = findViewById(R.id.tvSms)
        tvCall = findViewById(R.id.tvCall)
        tvPresenceTime = findViewById(R.id.tvPresenceTime)
        tvCallCountdown = findViewById(R.id.tvCallCountdown)
        tvNetworkInfo = findViewById(R.id.tvNetworkInfo)
    }

    private fun configureButtons() {
        btnTabMonitor.setOnClickListener { showTab(camera = false) }
        btnTabCamera.setOnClickListener { showTab(camera = true) }

        findViewById<MaterialButton>(R.id.btnReloadCamera).setOnClickListener {
            loadCamera()
        }

        findViewById<MaterialButton>(R.id.btnOpenCameraBrowser).setOnClickListener {
            val url = DeviceUrls.streamUrl(preferences.cameraBaseUrl)
            runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                .onFailure { Toast.makeText(this, "No se pudo abrir el navegador", Toast.LENGTH_LONG).show() }
        }

        findViewById<MaterialButton>(R.id.btnCameraSettings).setOnClickListener {
            showSettingsDialog()
        }

        findViewById<MaterialButton>(R.id.btnSendSms).setOnClickListener {
            executeCommand(
                action = "sms",
                params = mapOf("mensaje" to "PIA: mensaje de prueba enviado desde la APK.")
            )
        }

        findViewById<MaterialButton>(R.id.btnCall).setOnClickListener {
            executeCommand(action = "llamada")
        }

        findViewById<MaterialButton>(R.id.btnReset).setOnClickListener {
            executeCommand(action = "reset")
            handler.postDelayed({ requestStatus() }, 700)
        }

        findViewById<MaterialButton>(R.id.btnSettings).setOnClickListener {
            showSettingsDialog()
        }
    }

    private fun showTab(camera: Boolean) {
        monitorContainer.visibility = if (camera) android.view.View.GONE else android.view.View.VISIBLE
        cameraContainer.visibility = if (camera) android.view.View.VISIBLE else android.view.View.GONE
        btnTabMonitor.isEnabled = camera
        btnTabCamera.isEnabled = !camera
        if (camera) loadCamera()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureCameraWebView() {
        webCamera.setBackgroundColor(Color.BLACK)
        webCamera.settings.javaScriptEnabled = true
        webCamera.settings.domStorageEnabled = true
        webCamera.settings.mediaPlaybackRequiresUserGesture = false
        webCamera.settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        webCamera.settings.loadsImagesAutomatically = true
        webCamera.settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE

        webCamera.webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: android.webkit.WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    markCameraDisconnected()
                }
            }
        }
    }

    private fun loadCamera() {
        val cameraBaseUrl = DeviceUrls.normalizeBaseUrl(preferences.cameraBaseUrl)
        tvCameraConnection.text = "Cámara: comprobando…"
        tvCameraConnection.setTextColor(color(R.color.pia_muted))

        // Cargamos exactamente la misma página HTTP que funciona en Chrome.
        // Esto evita envolver el MJPEG en otro documento HTML, algo que algunos
        // Android System WebView no manejan bien con streams multipart.
        webCamera.stopLoading()
        webCamera.clearCache(true)
        webCamera.loadUrl(DeviceUrls.streamUrl(cameraBaseUrl))

        probeCamera(cameraBaseUrl)
    }

    private fun probeCamera(cameraBaseUrl: String) {
        executor.execute {
            val reachable = runCatching {
                val connection = (URL("$cameraBaseUrl/").openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 2500
                    readTimeout = 2500
                    useCaches = false
                }
                try {
                    connection.responseCode in 200..399
                } finally {
                    connection.disconnect()
                }
            }.getOrDefault(false)

            runOnUiThread {
                if (reachable) {
                    tvCameraConnection.text = "Cámara: conectada"
                    tvCameraConnection.setTextColor(color(R.color.pia_success))
                } else {
                    markCameraDisconnected()
                }
            }
        }
    }

    private fun markCameraDisconnected() {
        tvCameraConnection.text = "Cámara: sin conexión"
        tvCameraConnection.setTextColor(color(R.color.pia_danger))
    }

    private fun requestStatus() {
        if (!statusRequestInFlight.compareAndSet(false, true)) return
        val baseUrl = preferences.piaBaseUrl

        executor.execute {
            val result = apiClient.fetchStatus(baseUrl)
            runOnUiThread {
                statusRequestInFlight.set(false)
                result.onSuccess { updateStatus(it) }
                    .onFailure { markPiaDisconnected() }
            }
        }
    }

    private fun updateStatus(status: PiaStatus) {
        tvPiaConnection.text = "PIA: conectado"
        tvPiaConnection.setTextColor(color(R.color.pia_success))

        val (stateLabel, stateColor) = when (status.estado.lowercase(Locale.ROOT)) {
            "seguro" -> "ESTADO: SEGURO" to R.color.pia_success
            "alerta" -> "ESTADO: ALERTA POR PRESENCIA" to R.color.pia_warning
            "accidente" -> "ESTADO: IMPACTO / ACCIDENTE" to R.color.pia_danger
            "fuera_rango" -> "ESTADO: FUERA DE RANGO" to R.color.pia_warning
            else -> "ESTADO: ${status.estado.uppercase(Locale.ROOT)}" to R.color.pia_muted
        }

        tvGeneralState.text = stateLabel
        tvGeneralState.setTextColor(color(stateColor))
        tvPresence.text = "Presencia: ${if (status.presencia) "DETECTADA" else "No detectada"}"
        tvDistance.text = if (status.fueraRango) {
            "Distancia: fuera de rango"
        } else {
            "Distancia: ${status.distancia} cm"
        }
        tvTemperature.text = String.format(Locale.getDefault(), "Temperatura: %.1f °C", status.temperatura)
        tvHumidity.text = String.format(Locale.getDefault(), "Humedad: %.1f %%", status.humedad)
        tvImpact.text = "Impacto: ${if (status.impacto) "DETECTADO" else "Normal"}"
        tvSms.text = "SMS automático: ${if (status.smsEnviado) "enviado" else "pendiente/no requerido"}"
        tvCall.text = "Llamada automática: ${if (status.llamadaRealizada) "realizada" else "pendiente/no requerida"}"
        tvPresenceTime.text = "Tiempo con presencia: ${formatDuration(status.segundosPresencia)}"
        tvCallCountdown.text = if (status.presencia && status.llamadaEn > 0) {
            "Llamada automática en: ${status.llamadaEn} s"
        } else {
            "Llamada automática en: --"
        }

        val sta = if (status.wifi) {
            "WiFi ESP32: ${status.ssid.ifBlank { "conectado" }} ${status.ip}".trim()
        } else {
            "WiFi ESP32: sin conexión STA"
        }
        val ap = "AP: ${status.apSsid.ifBlank { "PIA_MONITOR" }} ${status.apIp.ifBlank { "192.168.4.1" }}"
        val phone = if (status.telefonoConfigurado) "Teléfono: configurado" else "Teléfono: no configurado"
        tvNetworkInfo.text = "$sta\n$ap\n$phone"
    }

    private fun markPiaDisconnected() {
        tvPiaConnection.text = "PIA: sin conexión"
        tvPiaConnection.setTextColor(color(R.color.pia_danger))
        tvNetworkInfo.text = "Sin respuesta de ${preferences.piaBaseUrl}"
    }

    private fun executeCommand(
        action: String,
        params: Map<String, String> = emptyMap()
    ) {
        val baseUrl = preferences.piaBaseUrl
        executor.execute {
            val result = apiClient.sendCommand(baseUrl, action, params)
            runOnUiThread {
                result.onSuccess {
                    Toast.makeText(this, commandSuccessMessage(action), Toast.LENGTH_SHORT).show()
                    requestStatus()
                }.onFailure { error ->
                    Toast.makeText(
                        this,
                        "No se pudo ejecutar $action: ${error.message ?: "sin respuesta"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun commandSuccessMessage(action: String): String = when (action) {
        "sms" -> "Comando SMS enviado"
        "llamada" -> "Comando de llamada enviado"
        "reset" -> "Alertas reiniciadas"
        "telefono" -> "Teléfono guardado en el ESP32"
        else -> "Comando ejecutado"
    }

    private fun showSettingsDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_settings, null)
        val piaInput = view.findViewById<TextInputEditText>(R.id.editPiaUrl)
        val cameraInput = view.findViewById<TextInputEditText>(R.id.editCameraUrl)
        val phoneInput = view.findViewById<TextInputEditText>(R.id.editPhone)

        piaInput.setText(preferences.piaBaseUrl)
        cameraInput.setText(preferences.cameraBaseUrl)
        phoneInput.setText(preferences.phoneNumber)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.settings_title)
            .setView(view)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val piaUrl = piaInput.text?.toString().orEmpty().trim()
                val cameraUrl = cameraInput.text?.toString().orEmpty().trim()
                val phone = phoneInput.text?.toString().orEmpty().trim()

                piaInput.error = null
                cameraInput.error = null
                phoneInput.error = null

                if (!isValidHttpUrl(piaUrl)) {
                    piaInput.error = "Usa una URL como http://192.168.4.1"
                    return@setOnClickListener
                }
                if (!isValidHttpUrl(cameraUrl)) {
                    cameraInput.error = "Usa una URL como http://192.168.4.2"
                    return@setOnClickListener
                }
                if (phone.isNotBlank() && phone.length < 8) {
                    phoneInput.error = "Número demasiado corto"
                    return@setOnClickListener
                }

                preferences.piaBaseUrl = piaUrl
                preferences.cameraBaseUrl = cameraUrl
                preferences.phoneNumber = phone
                dialog.dismiss()

                loadCamera()
                requestStatus()
                Toast.makeText(this, "Configuración guardada", Toast.LENGTH_SHORT).show()

                if (phone.isNotBlank()) {
                    executeCommand("telefono", mapOf("numero" to phone))
                }
            }
        }

        dialog.show()
    }

    private fun isValidHttpUrl(value: String): Boolean {
        val uri = Uri.parse(value)
        val schemeOk = uri.scheme.equals("http", ignoreCase = true) ||
            uri.scheme.equals("https", ignoreCase = true)
        return schemeOk && !uri.host.isNullOrBlank()
    }

    private fun formatDuration(seconds: Long): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return if (minutes > 0) {
            String.format(Locale.getDefault(), "%d min %02d s", minutes, remainingSeconds)
        } else {
            "$remainingSeconds s"
        }
    }

    private fun color(resourceId: Int): Int = ContextCompat.getColor(this, resourceId)

    companion object {
        private const val STATUS_INTERVAL_MS = 2000L
    }
}
