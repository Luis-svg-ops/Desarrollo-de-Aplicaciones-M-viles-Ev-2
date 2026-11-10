package cl.duoc.acis.view

import cl.duoc.acis.model.EstadoOT
import cl.duoc.acis.view.components.textoEstado
import org.junit.Assert.assertEquals
import org.junit.Test

class TextoEstadoTest {

    @Test
    fun clienteVeEnRevisionMientrasElJefeNoDaConformidad() {
        assertEquals("En revisión", textoEstado(EstadoOT.CERRADA, paraCliente = true))
        assertEquals("Cerrada", textoEstado(EstadoOT.CERRADA, paraCliente = false))
        assertEquals("Conforme", textoEstado(EstadoOT.CONFORME, paraCliente = true))
    }
}
