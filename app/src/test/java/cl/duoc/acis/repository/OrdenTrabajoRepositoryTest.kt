package cl.duoc.acis.repository

import cl.duoc.acis.Muestras
import cl.duoc.acis.data.EquipoDao
import cl.duoc.acis.data.EventoDao
import cl.duoc.acis.data.LocalDao
import cl.duoc.acis.data.OrdenTrabajoDao
import cl.duoc.acis.data.UsuarioDao
import cl.duoc.acis.data.remote.AccionRequest
import cl.duoc.acis.data.remote.EventoDto
import cl.duoc.acis.data.remote.MensajesRed
import cl.duoc.acis.data.remote.OrdenesApi
import cl.duoc.acis.data.remote.Resultado
import cl.duoc.acis.model.AccionOT
import cl.duoc.acis.model.EstadoOT
import cl.duoc.acis.model.OrdenTrabajo
import cl.duoc.acis.model.Rol
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Repositorio de OT con MockK: los DAO de Room y la API de ms-ordenes son falsos (mocks).
 * Así se prueba solo la lógica del repositorio: qué llama, en qué orden y qué devuelve.
 */
class OrdenTrabajoRepositoryTest {

    // relaxed = true: los métodos que no se configuran no hacen nada (en vez de fallar)
    private val otDao = mockk<OrdenTrabajoDao>(relaxed = true)
    private val equipoDao = mockk<EquipoDao>(relaxed = true)
    private val eventoDao = mockk<EventoDao>(relaxed = true)
    private val usuarioDao = mockk<UsuarioDao>(relaxed = true)
    private val localDao = mockk<LocalDao>(relaxed = true)
    private val api = mockk<OrdenesApi>()

    private val conServidor = OrdenTrabajoRepository(otDao, equipoDao, eventoDao, usuarioDao, localDao, api)
    private val soloRoom = OrdenTrabajoRepository(otDao, equipoDao, eventoDao, usuarioDao)

    // ---------- Con servidor (ms-ordenes) ----------

    @Test
    fun sincronizar_guardaLaCopiaEnRoom_yBorraLasQueYaNoVienen() = runTest {
        // Arrange: el servidor entrega 2 OT
        coEvery { api.ordenes() } returns listOf(Muestras.otDto(id = 2), Muestras.otDto(id = 5))
        coEvery { usuarioDao.getById(1) } returns null
        // Act
        val r = conServidor.sincronizar()
        // Assert
        assertTrue(r is Resultado.Exito)
        coVerify { localDao.upsertAll(any()) }
        coVerify { usuarioDao.upsert(match { it.id == 1L && it.nombre == "Pedro Soto" }) }
        coVerify(exactly = 2) { otDao.upsertAll(any()) }
        coVerify { otDao.borrarExcepto(listOf(2L, 5L)) }
    }

    @Test
    fun sincronizar_sinSenal_noTocaRoom_yAvisa() = runTest {
        coEvery { api.ordenes() } throws IOException("sin red")
        val r = conServidor.sincronizar()
        assertEquals(MensajesRed.SIN_CONEXION, (r as Resultado.Error).mensaje)
        assertTrue(r.sinConexion)
        coVerify(exactly = 0) { otDao.borrarExcepto(any()) }
    }

    @Test
    fun aceptar_mandaLaAccionAlServidor_yGuardaLaRespuesta() = runTest {
        val pedido = slot<AccionRequest>()
        coEvery { api.accion(2, capture(pedido)) } returns Muestras.otDto(EstadoOT.ACEPTADA)
        coEvery { api.historial(2) } returns listOf(EventoDto(1, EstadoOT.ACEPTADA, "Aceptada por Pedro Soto", 10))

        val error = conServidor.aplicar(2, AccionOT.ACEPTAR, Rol.TECNICO, 1)

        assertNull(error)
        assertEquals("ACEPTAR", pedido.captured.accion)
        coVerify { otDao.upsertAll(match { it.single().estado == EstadoOT.ACEPTADA }) }
        coVerify { eventoDao.insertAll(match { it.single().descripcion == "Aceptada por Pedro Soto" }) }
    }

    @Test
    fun siElServidorRechaza_seDevuelveSuMensaje_yNoSeGuardaNada() = runTest {
        coEvery { api.cierre(2, any()) } throws Muestras.errorHttp(409, "Acción no permitida: la OT está aceptada")
        val error = conServidor.cerrar(2, 1, "Diagnóstico largo", "Actividades largas", "", "content://foto")
        assertEquals("Acción no permitida: la OT está aceptada", error)
        coVerify(exactly = 0) { otDao.upsertAll(any()) }
    }

    @Test
    fun checkIn_crear_editar_vanAlServidor() = runTest {
        coEvery { api.checkIn(2, any()) } returns Muestras.otDto(EstadoOT.EN_EJECUCION)
        coEvery { api.crear(any()) } returns Muestras.otDto()
        coEvery { api.editar(2, any()) } returns Muestras.otDto()
        coEvery { api.historial(any()) } returns emptyList()

        assertNull(conServidor.checkIn(2, 1, -33.4, -70.6))
        assertNull(conServidor.guardarNueva(Muestras.ot()))
        assertNull(conServidor.actualizar(Muestras.ot()))
        coVerify { api.checkIn(2, match { it.latitud == -33.4 }) }
    }

    @Test
    fun historial_sinSenal_conservaElGuardado() = runTest {
        coEvery { api.historial(2) } throws IOException()
        conServidor.sincronizarHistorial(2)
        coVerify(exactly = 0) { eventoDao.borrarDeOt(any()) }
    }

    // ---------- Solo Room (sin servidor: como antes de la EFT) ----------

    private fun conOtEnRoom(ot: OrdenTrabajo) {
        every { otDao.getById(ot.id) } returns flowOf(ot)
        coEvery { usuarioDao.getById(1) } returns Muestras.pedro
    }

    @Test
    fun local_lasReglasLasRevisaLaMaquinaDeEstados() = runTest {
        conOtEnRoom(Muestras.ot(EstadoOT.ASIGNADA))
        assertEquals("Tu rol no permite esta acción", soloRoom.aplicar(2, AccionOT.CONFORMIDAD, Rol.TECNICO, 1))
        assertEquals("Esta OT no está asignada a ti", soloRoom.aplicar(2, AccionOT.ACEPTAR, Rol.TECNICO, 9))
        assertNull(soloRoom.aplicar(2, AccionOT.ACEPTAR, Rol.TECNICO, 1))
        coVerify { otDao.update(match { it.estado == EstadoOT.ACEPTADA && it.fechaAceptacion != null }) }
        coVerify { eventoDao.insert(match { it.descripcion == "Aceptada por Pedro Soto" }) }
    }

    @Test
    fun local_cadaAccionDejaSuTextoEnElHistorial() = runTest {
        val casos = listOf(
            Triple(AccionOT.RECHAZAR, EstadoOT.ASIGNADA, "Rechazada por Pedro Soto: Fuera de mi zona"),
            Triple(AccionOT.VISITA_FALLIDA, EstadoOT.ACEPTADA, "Visita fallida: Local cerrado"),
            Triple(AccionOT.CONFORMIDAD, EstadoOT.CERRADA, "Conformidad de la jefatura"),
            Triple(AccionOT.DISCONFORMIDAD, EstadoOT.CERRADA, "Disconformidad de la jefatura: Trabajo incompleto"),
            Triple(AccionOT.POSTERGAR, EstadoOT.ASIGNADA, "Postergada: Falta de repuesto"),
            Triple(AccionOT.CANCELAR, EstadoOT.ASIGNADA, "Cancelada: OT duplicada"),
        )
        val motivos = mapOf(AccionOT.RECHAZAR to "Fuera de mi zona", AccionOT.VISITA_FALLIDA to "Local cerrado",
            AccionOT.DISCONFORMIDAD to "Trabajo incompleto", AccionOT.POSTERGAR to "Falta de repuesto", AccionOT.CANCELAR to "OT duplicada")
        casos.forEach { (accion, estado, texto) ->
            conOtEnRoom(Muestras.ot(estado))
            assertNull(soloRoom.aplicar(2, accion, accion.rol, 1, motivos[accion]))
            coVerify { eventoDao.insert(match { it.descripcion == texto }) }
        }
    }

    @Test
    fun local_checkIn_yCierre_guardanSusDatos() = runTest {
        conOtEnRoom(Muestras.ot(EstadoOT.ACEPTADA))
        assertNull(soloRoom.checkIn(2, 1, -33.4, -70.6))
        coVerify { otDao.update(match { it.checkInLat == -33.4 && it.estado == EstadoOT.EN_EJECUCION }) }

        conOtEnRoom(Muestras.ot(EstadoOT.EN_EJECUCION))
        coEvery { equipoDao.getById(3) } returns Muestras.equipo
        assertNull(soloRoom.cerrar(2, 1, "Diagnóstico largo", "Actividades largas", "", "content://foto"))
        coVerify { otDao.update(match { it.estado == EstadoOT.CERRADA && it.materialesUtilizados == null }) }
        // La OT cerrada actualiza la última mantención del equipo
        coVerify { equipoDao.update(match { it.fechaUltimaMantencion != null }) }
    }

    @Test
    fun local_crear_asignaNumero_yRegistraElHistorial() = runTest {
        coEvery { otDao.insert(any()) } returns 8
        coEvery { usuarioDao.getById(1) } returns Muestras.pedro
        assertNull(soloRoom.guardarNueva(Muestras.ot(tecnicoId = 1, id = 0)))
        coVerify { otDao.update(match { it.id == 8L && it.numero.endsWith("0108") }) }
        coVerify { eventoDao.insert(match { it.descripcion == "Asignada a Pedro Soto" }) }
    }

    @Test
    fun local_editar_reasignar_yQuitarTecnico() = runTest {
        conOtEnRoom(Muestras.ot(EstadoOT.ACEPTADA, tecnicoId = 1))
        coEvery { usuarioDao.getById(3) } returns Muestras.pedro.copy(id = 3, nombre = "Matías Rojas")
        // Reasignada: vuelve a "Asignada" y hay que aceptarla de nuevo
        assertNull(soloRoom.actualizar(Muestras.ot(EstadoOT.ACEPTADA, tecnicoId = 3)))
        coVerify { eventoDao.insert(match { it.descripcion == "Asignada a Matías Rojas" }) }
        // Sin técnico: vuelve a "Creada"
        assertNull(soloRoom.actualizar(Muestras.ot(EstadoOT.ACEPTADA, tecnicoId = null)))
        coVerify { eventoDao.insert(match { it.descripcion == "Técnico quitado por la jefatura" }) }
        // Una OT cerrada ya no se edita
        conOtEnRoom(Muestras.ot(EstadoOT.CERRADA))
        assertEquals("Una OT cerrada no se puede editar", soloRoom.actualizar(Muestras.ot(EstadoOT.CERRADA)))
    }

    @Test
    fun local_otInexistente_avisa() = runTest {
        every { otDao.getById(99) } returns flowOf(null)
        assertEquals("La OT ya no existe", soloRoom.aplicar(99, AccionOT.ACEPTAR, Rol.TECNICO, 1))
        assertEquals("La OT ya no existe", soloRoom.actualizar(Muestras.ot(id = 99)))
    }

    @Test
    fun cadaRolLeeSuConsulta() {
        every { otDao.getAllConDetalle() } returns flowOf(emptyList())
        every { otDao.getByTecnicoConDetalle(1) } returns flowOf(emptyList())
        every { otDao.getByLocalConDetalle(2) } returns flowOf(emptyList())
        soloRoom.getParaUsuario(1, Rol.JEFE)
        soloRoom.getParaUsuario(1, Rol.ADMIN)
        soloRoom.getParaUsuario(1, Rol.TECNICO)
        soloRoom.getParaUsuario(6, Rol.CLIENTE, 2)
        io.mockk.verify(exactly = 2) { otDao.getAllConDetalle() }
        io.mockk.verify { otDao.getByTecnicoConDetalle(1) }
        io.mockk.verify { otDao.getByLocalConDetalle(2) }
    }
}
