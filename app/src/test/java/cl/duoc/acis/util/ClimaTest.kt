package cl.duoc.acis.util

import cl.duoc.acis.data.remote.ClimaResponse
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Clima (API externa Open-Meteo): lectura del JSON, traducción del código y recomendación. */
class ClimaTest {

    @Test
    fun gson_leeElJsonRealDeOpenMeteo() {
        // Arrange: respuesta tal como la entrega Open-Meteo (resumida)
        val json = """{"latitude":-33.5,"current_units":{"temperature_2m":"°C"},
            "current":{"time":"2026-11-26T15:00","interval":900,"temperature_2m":24.6,
            "precipitation":0.0,"weather_code":1,"wind_speed_10m":12.3}}"""
        // Act
        val actual = Gson().fromJson(json, ClimaResponse::class.java).actual
        // Assert: @SerializedName conectó temperature_2m → temperatura, etc.
        assertEquals(24.6, actual.temperatura, 0.01)
        assertEquals(1, actual.codigo)
        assertEquals(12.3, actual.vientoKmh, 0.01)
    }

    @Test
    fun codigoWmo_seTraduceAlEspanol() {
        assertEquals("Despejado", descripcionClima(0))
        assertEquals("Lluvia", descripcionClima(63))
        assertEquals("Tormenta", descripcionClima(95))
        assertEquals("Sin datos", descripcionClima(999))
    }

    @Test
    fun lluviaEnEquipoExterior_recomiendaPostergar() {
        val lluvia = crearClima(12.0, codigo = 61, vientoKmh = 10.0, lluviaMm = 1.2)
        assertTrue(recomendacionClima(lluvia, equipoExterior = true)!!.contains("postergar"))
        // Equipo interior: la lluvia no afecta el trabajo
        assertNull(recomendacionClima(lluvia, equipoExterior = false))
    }

    @Test
    fun vientoFuerte_avisaQueNoEsSeguroTrabajarEnAltura() {
        val viento = crearClima(18.0, codigo = 1, vientoKmh = 45.4, lluviaMm = 0.0)
        assertEquals("Viento fuerte (45 km/h): trabajar en altura no es seguro", recomendacionClima(viento, true))
    }

    @Test
    fun diaDespejado_sinAdvertencias() {
        assertNull(recomendacionClima(crearClima(25.0, 0, 8.0, 0.0), equipoExterior = true))
    }
}
