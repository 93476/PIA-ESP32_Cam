package mx.conalep.pia

import org.json.JSONObject

data class PiaStatus(
    val wifi: Boolean = false,
    val ssid: String = "",
    val ip: String = "",
    val apSsid: String = "",
    val apIp: String = "",
    val telefonoConfigurado: Boolean = false,
    val estado: String = "desconocido",
    val presencia: Boolean = false,
    val fueraRango: Boolean = false,
    val distancia: Int = 0,
    val temperatura: Double = 0.0,
    val humedad: Double = 0.0,
    val impacto: Boolean = false,
    val smsEnviado: Boolean = false,
    val llamadaRealizada: Boolean = false,
    val segundosPresencia: Long = 0,
    val llamadaEn: Long = 0
) {
    companion object {
        fun fromJson(json: String): PiaStatus {
            val obj = JSONObject(json)
            return PiaStatus(
                wifi = obj.optBoolean("wifi", false),
                ssid = obj.optString("ssid", ""),
                ip = obj.optString("ip", ""),
                apSsid = obj.optString("apSsid", ""),
                apIp = obj.optString("apIp", ""),
                telefonoConfigurado = obj.optBoolean("telefonoConfigurado", false),
                estado = obj.optString("estado", "desconocido"),
                presencia = obj.optBoolean("presencia", false),
                fueraRango = obj.optBoolean("fueraRango", false),
                distancia = obj.optInt("distancia", 0),
                temperatura = obj.optDouble("temperatura", 0.0),
                humedad = obj.optDouble("humedad", 0.0),
                impacto = obj.optBoolean("impacto", false),
                smsEnviado = obj.optBoolean("smsEnviado", false),
                llamadaRealizada = obj.optBoolean("llamadaRealizada", false),
                segundosPresencia = obj.optLong("segundosPresencia", 0L),
                llamadaEn = obj.optLong("llamadaEn", 0L)
            )
        }
    }
}
