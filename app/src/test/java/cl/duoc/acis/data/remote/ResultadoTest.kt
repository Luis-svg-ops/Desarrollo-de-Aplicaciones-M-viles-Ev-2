package cl.duoc.acis.data.remote

import cl.duoc.acis.Muestras
import cl.duoc.acis.model.EstadoOT
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** llamarApi: ninguna excepción llega a la pantalla, se convierte en un mensaje claro (try-catch). */
class ResultadoTest {

    @Test
    fun exito_devuelveLosDatos() = runTest {
        assertEquals(42, (llamarApi { 42 } as Resultado.Exito).datos)
    }

    @Test
    fun sinRed_esSinConexion() = runTest {
        val r = llamarApi { throw IOException("timeout") } as Resultado.Error
        assertEquals(MensajesRed.SIN_CONEXION, r.mensaje)
        assertTrue(r.sinConexion)
    }

    @Test
    fun errorDelServidor_seMuestraSuMensaje() = runTest {
        val r = llamarApi { throw Muestras.errorHttp(409, "Ese correo ya está registrado") } as Resultado.Error
        assertEquals("Ese correo ya está registrado", r.mensaje)
    }

    @Test
    fun sinCuerpo_seUsaUnMensajeSegunElCodigo() = runTest {
        assertEquals(MensajesRed.SESION_EXPIRADA, (llamarApi { throw Muestras.errorHttp(401) } as Resultado.Error).mensaje)
        assertEquals("No tienes permiso para esta acción", (llamarApi { throw Muestras.errorHttp(403) } as Resultado.Error).mensaje)
        assertEquals(MensajesRed.DESCONOCIDO, (llamarApi { throw Muestras.errorHttp(500) } as Resultado.Error).mensaje)
    }

    @Test
    fun cualquierOtroError_esInesperado() = runTest {
        assertEquals(MensajesRed.DESCONOCIDO, (llamarApi { error("JSON raro") } as Resultado.Error).mensaje)
    }

    @Test
    fun traductor_dtoAEntidad_yEntidadAPedido() {
        val ot = Muestras.otDto(EstadoOT.CERRADA).aEntidad()
        assertEquals(2L, ot.localId)
        assertEquals(3L, ot.equipoId)
        assertEquals(EstadoOT.CERRADA, ot.estado)
        assertEquals(3L, ot.aPedido().equipoId)
        assertEquals("Daikin", Muestras.equipo.aPedido().marca)
        assertEquals("sofia@correo.cl", Muestras.usuarioDto().aEntidad().email)
        assertEquals("usuario4@acis.local", tecnicoDeOt(4, null).email)
        assertEquals("Aceptada", EventoDto(1, EstadoOT.ACEPTADA, "Aceptada", 5).aEntidad(2).descripcion)
    }
}
