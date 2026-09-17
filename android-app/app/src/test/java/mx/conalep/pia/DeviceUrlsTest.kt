package mx.conalep.pia

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceUrlsTest {

    @Test
    fun normalizeBaseUrl_removesTrailingSlashes() {
        assertEquals("http://192.168.4.1", DeviceUrls.normalizeBaseUrl("http://192.168.4.1///"))
    }

    @Test
    fun statusUrl_buildsExpectedEndpoint() {
        assertEquals(
            "http://192.168.4.1/api/status",
            DeviceUrls.statusUrl("http://192.168.4.1/")
        )
    }

    @Test
    fun commandUrl_encodesActionAndParameters() {
        assertEquals(
            "http://192.168.4.1/api/cmd?accion=sms&mensaje=Hola+desde+PIA",
            DeviceUrls.commandUrl(
                baseUrl = "http://192.168.4.1",
                action = "sms",
                params = linkedMapOf("mensaje" to "Hola desde PIA")
            )
        )
    }

    @Test
    fun streamUrl_buildsExpectedCameraEndpoint() {
        assertEquals(
            "http://192.168.4.2/stream",
            DeviceUrls.streamUrl("http://192.168.4.2/")
        )
    }
}
