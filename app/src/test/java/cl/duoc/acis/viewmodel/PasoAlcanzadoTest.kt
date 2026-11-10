package cl.duoc.acis.viewmodel

import cl.duoc.acis.model.EstadoOT
import cl.duoc.acis.model.OrdenTrabajo
import cl.duoc.acis.model.Prioridad
import cl.duoc.acis.model.TipoOT
import org.junit.Assert.assertEquals
import org.junit.Test

/** Hasta dónde llegó una OT antes de detenerse (para marcar en rojo el paso correcto). */
class PasoAlcanzadoTest {

    private fun ot(estado: EstadoOT, aceptada: Boolean = false, checkIn: Boolean = false) = OrdenTrabajo(
        numero = "OT-1", localId = 1, equipoId = 1, tecnicoId = 1, tipo = TipoOT.CORRECTIVA,
        prioridad = Prioridad.MEDIA, estado = estado, descripcion = "Prueba de OT",
        fechaCreacion = 0, fechaProgramada = 0,
        fechaAceptacion = if (aceptada) 1L else null,
        checkInFechaHora = if (checkIn) 2L else null
    )

    @Test
    fun postergadaAntesDeAceptarSeDetieneEnAsignada() {
        assertEquals(0, pasoAlcanzado(ot(EstadoOT.POSTERGADA)))
    }

    @Test
    fun postergadaDespuesDeAceptarSeDetieneEnAceptada() {
        assertEquals(1, pasoAlcanzado(ot(EstadoOT.POSTERGADA, aceptada = true)))
    }

    @Test
    fun conCheckInLlegoAEjecucion() {
        assertEquals(2, pasoAlcanzado(ot(EstadoOT.EN_EJECUCION, aceptada = true, checkIn = true)))
    }
}
