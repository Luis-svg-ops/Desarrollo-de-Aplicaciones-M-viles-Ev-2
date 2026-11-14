package cl.duoc.acis.util

import cl.duoc.acis.model.Equipo
import cl.duoc.acis.model.EstadoOT
import cl.duoc.acis.model.EventoOT
import cl.duoc.acis.model.Local
import cl.duoc.acis.model.OrdenTrabajo
import cl.duoc.acis.model.OtConDetalle
import cl.duoc.acis.model.Prioridad
import cl.duoc.acis.model.Rol
import cl.duoc.acis.model.TipoEquipo
import cl.duoc.acis.model.TipoOT
import cl.duoc.acis.model.UbicacionEquipo
import cl.duoc.acis.model.Usuario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Informe de la OT con Gson: objeto → JSON → objeto, sin perder datos. */
class InformeOtTest {

    private val minuto = 60_000L
    private val local = Local(2, "Mall Costanera Oeste", "Av. Américo Vespucio 1501", "Cerrillos", "Andrea Pino", "+56 9 7345 1122")
    private val equipo = Equipo(3, 2, TipoEquipo.CLIMATIZACION, "Daikin", "Chiller EWAD-TZ", UbicacionEquipo.EXTERIOR)
    private val pedro = Usuario(1, "Pedro Soto", "tecnico@acis.cl", "123456", Rol.TECNICO)

    private fun otCerrada() = OtConDetalle(
        OrdenTrabajo(
            id = 2, numero = "OT-2026-0102", localId = 2, equipoId = 3, tecnicoId = 1,
            tipo = TipoOT.PREVENTIVA, prioridad = Prioridad.MEDIA, estado = EstadoOT.CONFORME,
            descripcion = "Mantención preventiva", fechaCreacion = 0, fechaProgramada = 1_000 * minuto,
            checkInFechaHora = 1_000 * minuto, checkInLat = -33.511, checkInLon = -70.7575,
            checkOutFechaHora = 1_052 * minuto, diagnostico = "Serpentín sucio",
            actividadesRealizadas = "Limpieza de serpentines", fotoUri = "content://x"
        ),
        local, equipo, pedro
    )

    private val historial = listOf(
        EventoOT(otId = 2, estado = EstadoOT.CREADA, descripcion = "Creada por la jefatura", fecha = 900 * minuto),
        EventoOT(otId = 2, estado = EstadoOT.CONFORME, descripcion = "Conformidad de la jefatura", fecha = 1_060 * minuto)
    )

    @Test
    fun informe_tieneLosDatosLegiblesDeLaOt() {
        // Dado una OT conforme con su historial
        // Cuando armo el informe
        val informe = crearInforme(otCerrada(), historial)
        // Entonces tiene textos legibles (no ids) y la duración calculada
        assertEquals("OT-2026-0102", informe.numero)
        assertEquals("Conforme", informe.estado)
        assertEquals("Mall Costanera Oeste", informe.local.nombre)
        assertEquals("Exterior / azotea", informe.equipo.ubicacion)
        assertEquals("Pedro Soto", informe.tecnico)
        assertEquals("52 min", informe.duracion)
        assertEquals(-33.511, informe.ubicacionCheckIn!!.latitud, 0.0001)
        assertTrue(informe.tieneFotoDeEvidencia)
        assertEquals(2, informe.historial.size)
    }

    @Test
    fun gson_convierteAJsonYDeVueltaSinPerderNada() {
        // Arrange
        val informe = crearInforme(otCerrada(), historial, ahora = 1_100 * minuto)
        // Act
        val json = InformeJson.aJson(informe)
        val leido = InformeJson.desdeJson(json)
        // Assert
        assertTrue(json.contains("\"numero\": \"OT-2026-0102\""))
        assertTrue("Formato legible con saltos de línea", json.contains("\n"))
        assertEquals(informe, leido)
    }

    @Test
    fun otSinTerminar_noEscribeLosDatosVacios() {
        // Dado una OT que todavía no tiene check-out
        val abierta = otCerrada().let { it.copy(ot = it.ot.copy(checkOutFechaHora = null, diagnostico = null)) }
        // Cuando la convierto a JSON
        val informe = crearInforme(abierta, emptyList())
        val json = InformeJson.aJson(informe)
        // Entonces no hay duración ni check-out, y Gson no escribe los campos vacíos
        assertNull(informe.duracion)
        assertFalse(json.contains("checkOut"))
        assertFalse(json.contains("diagnostico"))
    }

    @Test
    fun nombreDelArchivo_llevaElNumeroDeLaOt() {
        assertEquals("informe_OT-2026-0102.json", nombreArchivoInforme("OT-2026-0102"))
    }
}
