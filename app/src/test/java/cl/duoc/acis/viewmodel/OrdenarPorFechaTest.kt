package cl.duoc.acis.viewmodel

import cl.duoc.acis.model.Equipo
import cl.duoc.acis.model.EstadoOT
import cl.duoc.acis.model.Local
import cl.duoc.acis.model.OrdenTrabajo
import cl.duoc.acis.model.OtConDetalle
import cl.duoc.acis.model.Prioridad
import cl.duoc.acis.model.TipoEquipo
import cl.duoc.acis.model.TipoOT
import cl.duoc.acis.model.UbicacionEquipo
import org.junit.Assert.assertEquals
import org.junit.Test

class OrdenarPorFechaTest {

    private val local = Local(1, "Local", "Calle 1", "Maipú", "Ana", "+56 9 1111 1111")
    private val equipo = Equipo(1, 1, TipoEquipo.CLIMATIZACION, "Carrier", "Split", UbicacionEquipo.INTERIOR)

    private fun ot(numero: String, estado: EstadoOT, programada: Long, checkOut: Long? = null) = OtConDetalle(
        OrdenTrabajo(
            numero = numero, localId = 1, equipoId = 1, tipo = TipoOT.CORRECTIVA, prioridad = Prioridad.MEDIA,
            estado = estado, descripcion = "Prueba de orden", fechaCreacion = 0,
            fechaProgramada = programada, checkOutFechaHora = checkOut
        ),
        local, equipo, null
    )

    @Test
    fun abiertasPrimeroDeLaMasProxima_terminadasDespuesDeLaMasReciente() {
        // Arrange: una lista desordenada
        val lista = listOf(
            ot("cerrada-vieja", EstadoOT.CERRADA, programada = 100, checkOut = 150),
            ot("abierta-lejana", EstadoOT.ASIGNADA, programada = 900),
            ot("conforme-reciente", EstadoOT.CONFORME, programada = 200, checkOut = 800),
            ot("abierta-proxima", EstadoOT.ACEPTADA, programada = 300),
            ot("cancelada", EstadoOT.CANCELADA, programada = 500)
        )

        // Act
        val ordenada = ordenarPorFecha(lista).map { it.ot.numero }

        // Assert
        assertEquals(
            listOf("abierta-proxima", "abierta-lejana", "conforme-reciente", "cancelada", "cerrada-vieja"),
            ordenada
        )
    }

    @Test
    fun listaVaciaQuedaVacia() {
        assertEquals(emptyList<OtConDetalle>(), ordenarPorFecha(emptyList()))
    }
}
