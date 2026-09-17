package mx.conalep.pia

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class PiaApiClient(
    private val connectTimeoutMs: Int = 3500,
    private val readTimeoutMs: Int = 5000
) {
    fun fetchStatus(baseUrl: String): Result<PiaStatus> = runCatching {
        PiaStatus.fromJson(get(DeviceUrls.statusUrl(baseUrl)))
    }

    fun sendCommand(
        baseUrl: String,
        action: String,
        params: Map<String, String> = emptyMap()
    ): Result<String> = runCatching {
        get(DeviceUrls.commandUrl(baseUrl, action, params))
    }

    private fun get(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            useCaches = false
            setRequestProperty("Accept", "application/json")
        }

        return try {
            val statusCode = connection.responseCode
            val stream = if (statusCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (statusCode !in 200..299) {
                throw IOException("HTTP $statusCode${if (body.isNotBlank()) ": $body" else ""}")
            }

            body
        } finally {
            connection.disconnect()
        }
    }
}
