package cl.duoc.acis.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test

class InicialesTest {

    @Test
    fun inicialesDeNombreYApellido() {
        assertEquals("PS", iniciales("Pedro Soto"))
        assertEquals("CM", iniciales("Carolina Muñoz  Pérez"))
    }
}
