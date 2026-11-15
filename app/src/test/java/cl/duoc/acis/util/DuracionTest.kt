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

    @Test
    fun duracionDeLaOt_soloSiTieneCheckInYCheckOut() {
        val ot = cl.duoc.acis.model.OrdenTrabajo(
            numero = "OT-1", localId = 1, equipoId = 1, tipo = cl.duoc.acis.model.TipoOT.CORRECTIVA,
            prioridad = cl.duoc.acis.model.Prioridad.MEDIA, estado = cl.duoc.acis.model.EstadoOT.CERRADA,
            descripcion = "Prueba", fechaCreacion = 0, fechaProgramada = 0,
            checkInFechaHora = 10 * minuto, checkOutFechaHora = 62 * minuto
        )
        assertEquals("52 min", duracionAtencion(ot))
        assertEquals(null, duracionAtencion(ot.copy(checkOutFechaHora = null)))
    }
}
