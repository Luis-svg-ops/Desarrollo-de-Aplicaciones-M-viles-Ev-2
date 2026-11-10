package cl.duoc.acis.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Prueba las rutas de la OT: las permitidas y las prohibidas. */
class MaquinaEstadosOTTest {

    private val tecnico = 1L

    // ---- Rutas permitidas ----

    @Test
    fun tecnicoAceptaOtAsignada() {
        assertNull(MaquinaEstadosOT.validar(AccionOT.ACEPTAR, EstadoOT.ASIGNADA, Rol.TECNICO, null, tecnico, tecnico))
    }

    @Test
    fun tecnicoRechazaConMotivo() {
        assertNull(
            MaquinaEstadosOT.validar(AccionOT.RECHAZAR, EstadoOT.ASIGNADA, Rol.TECNICO, "Fuera de mi zona", tecnico, tecnico)
        )
    }

    @Test
    fun jefeCancelaOtDetenida() {
        assertNull(MaquinaEstadosOT.validar(AccionOT.CANCELAR, EstadoOT.VISITA_FALLIDA, Rol.JEFE, "OT duplicada"))
    }

    // ---- Rutas prohibidas ----

    @Test
    fun noSeAceptaUnaOtCerrada() {
        assertEquals(
            "Acción no permitida: la OT está cerrada",
            MaquinaEstadosOT.validar(AccionOT.ACEPTAR, EstadoOT.CERRADA, Rol.TECNICO, null, tecnico, tecnico)
        )
    }

    @Test
    fun jefeNoPuedeAceptar() {
        assertNotNull(MaquinaEstadosOT.validar(AccionOT.ACEPTAR, EstadoOT.ASIGNADA, Rol.JEFE))
    }

    @Test
    fun tecnicoNoTocaOtDeOtroTecnico() {
        assertEquals(
            "Esta OT no está asignada a ti",
            MaquinaEstadosOT.validar(AccionOT.ACEPTAR, EstadoOT.ASIGNADA, Rol.TECNICO, null, 2L, tecnico)
        )
    }

    @Test
    fun rechazarSinMotivoDaError() {
        assertEquals(
            "Selecciona un motivo",
            MaquinaEstadosOT.validar(AccionOT.RECHAZAR, EstadoOT.ASIGNADA, Rol.TECNICO, null, tecnico, tecnico)
        )
    }

    @Test
    fun motivoQueNoEstaEnLaListaDaError() {
        assertEquals(
            "Motivo no válido",
            MaquinaEstadosOT.validar(AccionOT.POSTERGAR, EstadoOT.ASIGNADA, Rol.JEFE, "Me dio flojera")
        )
    }

    @Test
    fun visitaFallidaSoloDespuesDeAceptar() {
        assertNotNull(
            MaquinaEstadosOT.validar(AccionOT.VISITA_FALLIDA, EstadoOT.ASIGNADA, Rol.TECNICO, "Local cerrado", tecnico, tecnico)
        )
    }

    @Test
    fun otEnEjecucionSoloPuedeCerrarse() {
        assertEquals(setOf(EstadoOT.CERRADA), MaquinaEstadosOT.destinos(EstadoOT.EN_EJECUCION))
    }

    // ---- Reglas generales de la tabla ----

    @Test
    fun estadosFinalesNoTienenSalida() {
        MaquinaEstadosOT.estadosFinales.forEach { assertTrue(MaquinaEstadosOT.destinos(it).isEmpty()) }
    }

    @Test
    fun todoEstadoNoFinalTieneSalida() {
        EstadoOT.entries.filter { it !in MaquinaEstadosOT.estadosFinales }
            .forEach { assertTrue("$it no tiene salida", MaquinaEstadosOT.destinos(it).isNotEmpty()) }
    }

    @Test
    fun todoEstadoSeAlcanzaDesdeAlgunaAccion() {
        // Ningún estado queda "huérfano" (Creada es el estado inicial)
        val alcanzables = AccionOT.entries.map { it.hacia }.toSet() + EstadoOT.CREADA
        assertEquals(EstadoOT.entries.toSet(), alcanzables)
    }

    @Test
    fun botonesDelTecnicoEnOtAsignada() {
        assertEquals(
            listOf(AccionOT.ACEPTAR, AccionOT.RECHAZAR),
            MaquinaEstadosOT.accionesDisponibles(EstadoOT.ASIGNADA, Rol.TECNICO)
        )
    }

    // ---- Edición desde el formulario ----

    @Test
    fun reasignarOtRechazadaVuelveAAsignada() {
        assertEquals(EstadoOT.ASIGNADA, MaquinaEstadosOT.estadoAlEditar(EstadoOT.RECHAZADA, tecnico, 2L))
    }

    @Test
    fun cambiarSoloDatosMantieneEstado() {
        assertEquals(EstadoOT.ACEPTADA, MaquinaEstadosOT.estadoAlEditar(EstadoOT.ACEPTADA, tecnico, tecnico))
    }

    @Test
    fun editarNuncaInventaUnaRuta() {
        // El estado que deja el formulario siempre es el mismo o uno permitido por la tabla
        val tecnicos = listOf(null, tecnico, 2L)
        EstadoOT.entries.filter { MaquinaEstadosOT.sePuedeEditar(it) }.forEach { estado ->
            tecnicos.forEach { antes ->
                tecnicos.forEach { despues ->
                    val nuevo = MaquinaEstadosOT.estadoAlEditar(estado, antes, despues)
                    assertTrue(
                        "$estado -> $nuevo no está en la tabla",
                        nuevo == estado || nuevo in MaquinaEstadosOT.destinos(estado)
                    )
                }
            }
        }
    }

    @Test
    fun otCerradaNoSeEdita() {
        assertTrue(!MaquinaEstadosOT.sePuedeEditar(EstadoOT.CERRADA))
    }

    @Test
    fun clienteSoloMiraNuncaTieneBotones() {
        EstadoOT.entries.forEach { assertTrue(MaquinaEstadosOT.accionesDisponibles(it, Rol.CLIENTE).isEmpty()) }
        assertNotNull(MaquinaEstadosOT.validar(AccionOT.CANCELAR, EstadoOT.ASIGNADA, Rol.CLIENTE, "OT duplicada"))
    }
}
