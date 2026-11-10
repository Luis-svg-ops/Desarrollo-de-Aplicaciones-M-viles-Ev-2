package cl.duoc.acis.util

import org.junit.Assert.assertEquals
import org.junit.Test

class DuracionTest {

    private val minuto = 60_000L

    @Test
    fun menosDeUnMinuto() {
        assertEquals("menos de 1 min", formatoDuracion(40_000))
    }

    @Test
    fun soloMinutos() {
        assertEquals("45 min", formatoDuracion(45 * minuto))
    }

    @Test
    fun horasYMinutos() {
        assertEquals("1 h 20 min", formatoDuracion(80 * minuto))
        assertEquals("2 h", formatoDuracion(120 * minuto))
    }
}
