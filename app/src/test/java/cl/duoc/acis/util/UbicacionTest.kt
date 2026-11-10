package cl.duoc.acis.util

import org.junit.Assert.assertEquals
import org.junit.Test

class UbicacionTest {

    @Test
    fun coordenadasConCincoDecimalesYPunto() {
        // Siempre con punto decimal (Locale.US), aunque el teléfono esté en español
        assertEquals("-33.51100, -70.75750", formatoCoordenadas(-33.511, -70.7575))
    }
}
