package mx.conalep.pia

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PiaStatusParserTest {

    @Test
    fun fromJson_parsesCompleteEsp32Status() {
        val json = """
            {
              "wifi": true,
              "ssid": "CONALEP055",
              "ip": "10.0.0.25",
              "apSsid": "PIA_MONITOR",
              "apIp": "192.168.4.1",
              "telefonoConfigurado": true,
              "estado": "alerta",
              "presencia": true,
              "fueraRango": false,
              "distancia": 43,
              "temperatura": 28.6,
              "humedad": 54.0,
              "impacto": false,
              "smsEnviado": true,
              "llamadaRealizada": false,
              "segundosPresencia": 18,
              "llamadaEn": 42
            }
        """.trimIndent()

        val status = PiaStatus.fromJson(json)

        assertTrue(status.wifi)
        assertEquals("CONALEP055", status.ssid)
        assertEquals("10.0.0.25", status.ip)
        assertEquals("PIA_MONITOR", status.apSsid)
        assertEquals("192.168.4.1", status.apIp)
        assertTrue(status.telefonoConfigurado)
        assertEquals("alerta", status.estado)
        assertTrue(status.presencia)
        assertFalse(status.fueraRango)
        assertEquals(43, status.distancia)
        assertEquals(28.6, status.temperatura, 0.01)
        assertEquals(54.0, status.humedad, 0.01)
        assertFalse(status.impacto)
        assertTrue(status.smsEnviado)
        assertFalse(status.llamadaRealizada)
        assertEquals(18L, status.segundosPresencia)
        assertEquals(42L, status.llamadaEn)
    }

    @Test
    fun fromJson_usesSafeDefaultsForMissingFields() {
        val status = PiaStatus.fromJson("{\"estado\":\"seguro\"}")

        assertEquals("seguro", status.estado)
        assertFalse(status.wifi)
        assertFalse(status.presencia)
        assertFalse(status.impacto)
        assertEquals(0, status.distancia)
        assertEquals(0.0, status.temperatura, 0.01)
        assertEquals(0L, status.segundosPresencia)
        assertEquals("", status.ssid)
    }
}
