package mx.conalep.pia

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object DeviceUrls {
    fun normalizeBaseUrl(value: String): String = value.trim().trimEnd('/')

    fun statusUrl(baseUrl: String): String = "${normalizeBaseUrl(baseUrl)}/api/status"

    fun streamUrl(baseUrl: String): String = "${normalizeBaseUrl(baseUrl)}/stream"

    fun commandUrl(
        baseUrl: String,
        action: String,
        params: Map<String, String> = emptyMap()
    ): String {
        val query = buildList {
            add("accion=${encode(action)}")
            params.forEach { (key, value) ->
                add("${encode(key)}=${encode(value)}")
            }
        }.joinToString("&")

        return "${normalizeBaseUrl(baseUrl)}/api/cmd?$query"
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
}
