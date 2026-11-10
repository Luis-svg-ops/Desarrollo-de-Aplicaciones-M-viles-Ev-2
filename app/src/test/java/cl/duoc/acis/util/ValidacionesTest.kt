package cl.duoc.acis.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ValidacionesTest {

    @Test
    fun emailVacioDaError() {
        assertEquals("Ingresa tu correo", validarEmail(""))
    }

    @Test
    fun emailSinArrobaDaError() {
        assertEquals("Formato de correo no válido (ej.: nombre@acis.cl)", validarEmail("tecnicoacis.cl"))
    }

    @Test
    fun emailValidoNoDaError() {
        assertNull(validarEmail("tecnico@acis.cl"))
    }

    @Test
    fun passwordCortaDaError() {
        assertEquals("Debe tener al menos 6 caracteres", validarPassword("123"))
    }

    @Test
    fun passwordValidaNoDaError() {
        assertNull(validarPassword("123456"))
    }
}
