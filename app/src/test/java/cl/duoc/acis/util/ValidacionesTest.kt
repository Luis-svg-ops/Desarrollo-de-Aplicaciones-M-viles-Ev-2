package cl.duoc.acis.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

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

    @Test
    fun seleccionVaciaDaError() {
        assertEquals("Selecciona un local", validarSeleccion(null, "Selecciona un local"))
        assertNull(validarSeleccion(1L, "Selecciona un local"))
    }

    @Test
    fun fechaPasadaDaError() {
        val hoy = LocalDate.of(2026, 11, 10)
        assertEquals("La fecha no puede ser anterior a hoy", validarFechaProgramada(hoy.minusDays(1), hoy))
        assertNull(validarFechaProgramada(hoy, hoy))
        assertEquals("Selecciona una fecha", validarFechaProgramada(null, hoy))
    }

    @Test
    fun descripcionCortaOLargaDaError() {
        assertEquals("Mínimo 10 caracteres (llevas 5)", validarDescripcion("corto"))
        assertEquals("Máximo 300 caracteres", validarDescripcion("a".repeat(301)))
        assertNull(validarDescripcion("Revisar presión del chiller"))
    }

    @Test
    fun diagnosticoVacioDaError() {
        assertEquals("Ingresa el diagnóstico", validarTextoCierre("  ", "el diagnóstico"))
    }

    @Test
    fun diagnosticoCortoDaError() {
        assertEquals("Mínimo 10 caracteres (llevas 4)", validarTextoCierre("ruid", "el diagnóstico"))
    }

    @Test
    fun diagnosticoValidoNoDaError() {
        assertNull(validarTextoCierre("Filtro saturado, se reemplazó", "el diagnóstico"))
    }

    @Test
    fun sinFotoNoSePuedeCerrar() {
        assertEquals("Toma una foto del equipo", validarFoto(null))
        assertNull(validarFoto("content://cl.duoc.acis.fotos/fotos/ot_1.jpg"))
    }
}
