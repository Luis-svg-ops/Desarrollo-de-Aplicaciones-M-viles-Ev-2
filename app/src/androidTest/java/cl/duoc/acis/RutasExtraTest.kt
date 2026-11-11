package cl.duoc.acis

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cl.duoc.acis.data.AppDatabase
import cl.duoc.acis.data.DatosEjemplo
import cl.duoc.acis.model.AccionOT
import cl.duoc.acis.model.Equipo
import cl.duoc.acis.model.EstadoOT
import cl.duoc.acis.model.MaquinaEstadosOT
import cl.duoc.acis.model.OrdenTrabajo
import cl.duoc.acis.model.Prioridad
import cl.duoc.acis.model.Rol
import cl.duoc.acis.model.TipoEquipo
import cl.duoc.acis.model.TipoOT
import cl.duoc.acis.model.UbicacionEquipo
import cl.duoc.acis.repository.EquipoRepository
import cl.duoc.acis.repository.OrdenTrabajoRepository
import cl.duoc.acis.repository.ResultadoBorrado
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Rutas poco comunes: lo que un usuario (o el profe) podría intentar para "romper" la app.
 * Igual que RutasOtTest: base real de Room en memoria con los datos de ejemplo.
 */
@RunWith(AndroidJUnit4::class)
class RutasExtraTest {

    private val pedro = 1L
    private val jefe = 2L
    private val matias = 3L
    private val cliente = 6L
    private val localCliente = 2L

    private lateinit var db: AppDatabase
    private lateinit var repo: OrdenTrabajoRepository
    private lateinit var equipos: EquipoRepository

    @Before
    fun crearBase() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        DatosEjemplo.cargar(db)
        repo = OrdenTrabajoRepository(db.ordenTrabajoDao(), db.equipoDao(), db.eventoDao(), db.usuarioDao())
        equipos = EquipoRepository(db.equipoDao(), db.ordenTrabajoDao())
    }

    @After
    fun cerrarBase() = db.close()

    // ---- Ayudas ----

    private suspend fun crearOt(tecnico: Long?, equipo: Long = 4, local: Long = localCliente): Long = repo.crear(
        OrdenTrabajo(
            numero = "", localId = local, equipoId = equipo, tecnicoId = tecnico,
            tipo = TipoOT.CORRECTIVA, prioridad = Prioridad.MEDIA,
            estado = MaquinaEstadosOT.estadoInicial(tecnico),
            descripcion = "Prueba de rutas extra", fechaCreacion = System.currentTimeMillis(),
            fechaProgramada = System.currentTimeMillis() + 3_600_000
        )
    )

    private suspend fun ot(id: Long) = repo.getOt(id).first()!!
    private suspend fun estado(id: Long) = ot(id).estado
    private suspend fun ve(usuario: Long, rol: Rol, id: Long) =
        repo.getParaUsuario(usuario, rol, if (rol == Rol.CLIENTE) localCliente else null).first().any { it.ot.id == id }
    private suspend fun historial(id: Long) = repo.getHistorial(id).first().map { it.descripcion }

    /** Lleva una OT nueva de Pedro hasta el estado pedido usando solo acciones permitidas. */
    private suspend fun otEn(destino: EstadoOT): Long {
        val id = crearOt(if (destino == EstadoOT.CREADA) null else pedro)
        when (destino) {
            EstadoOT.CREADA, EstadoOT.ASIGNADA -> Unit
            EstadoOT.RECHAZADA -> repo.aplicar(id, AccionOT.RECHAZAR, Rol.TECNICO, pedro, "Fuera de mi zona")
            EstadoOT.POSTERGADA -> repo.aplicar(id, AccionOT.POSTERGAR, Rol.JEFE, jefe, "Falta de repuesto")
            else -> {
                repo.aplicar(id, AccionOT.ACEPTAR, Rol.TECNICO, pedro)
                when (destino) {
                    EstadoOT.VISITA_FALLIDA -> repo.aplicar(id, AccionOT.VISITA_FALLIDA, Rol.TECNICO, pedro, "Encargado ausente")
                    EstadoOT.EN_EJECUCION, EstadoOT.CERRADA, EstadoOT.CONFORME -> {
                        repo.checkIn(id, pedro, -33.5, -70.7)
                        if (destino != EstadoOT.EN_EJECUCION) repo.cerrar(id, pedro, "Diagnóstico de prueba", "Actividad de prueba", "", "content://f")
                        if (destino == EstadoOT.CONFORME) repo.aplicar(id, AccionOT.CONFORMIDAD, Rol.JEFE, jefe)
                    }
                    EstadoOT.CANCELADA -> repo.aplicar(id, AccionOT.CANCELAR, Rol.JEFE, jefe, "OT duplicada")
                    else -> Unit
                }
            }
        }
        assertEquals("No se pudo preparar la OT en $destino", destino, estado(id))
        return id
    }

    // ---- Edición por el jefe ----

    @Test
    fun editarSinCambiarTecnico_mantieneAceptadaYSuFecha() = runBlocking {
        val id = otEn(EstadoOT.ACEPTADA)
        val aceptadaEn = ot(id).fechaAceptacion
        assertNull(repo.actualizar(ot(id).copy(descripcion = "Descripción corregida por el jefe")))
        assertEquals(EstadoOT.ACEPTADA, estado(id))
        assertEquals("No pierde la aceptación", aceptadaEn, ot(id).fechaAceptacion)
        assertEquals("Datos editados por la jefatura", historial(id).last())
    }

    @Test
    fun cambiarTecnicoDeOtAceptada_elNuevoDebeAceptar() = runBlocking {
        val id = otEn(EstadoOT.ACEPTADA)
        assertNull(repo.actualizar(ot(id).copy(tecnicoId = matias)))
        assertEquals(EstadoOT.ASIGNADA, estado(id))
        assertNull(ot(id).fechaAceptacion)
        assertFalse(ve(pedro, Rol.TECNICO, id))
        assertTrue(ve(matias, Rol.TECNICO, id))
        assertEquals("Asignada a Matías Rojas", historial(id).last())
    }

    @Test
    fun quitarTecnico_vuelveACreadaYElTecnicoLaPierde() = runBlocking {
        val id = otEn(EstadoOT.ASIGNADA)
        assertNull(repo.actualizar(ot(id).copy(tecnicoId = null)))
        assertEquals(EstadoOT.CREADA, estado(id))
        assertFalse(ve(pedro, Rol.TECNICO, id))
        assertTrue("El cliente la sigue viendo", ve(cliente, Rol.CLIENTE, id))
        assertEquals("Técnico quitado por la jefatura", historial(id).last())
    }

    @Test
    fun rechazadaReasignadaAlMismoTecnico_debeAceptarDeNuevo() = runBlocking {
        val id = otEn(EstadoOT.RECHAZADA)
        assertNull(repo.actualizar(ot(id).copy(fechaProgramada = System.currentTimeMillis() + 7_200_000)))
        assertEquals(EstadoOT.ASIGNADA, estado(id))
        assertNull("El motivo del rechazo ya no aplica", ot(id).motivo)
        assertEquals(setOf(AccionOT.ACEPTAR, AccionOT.RECHAZAR),
            MaquinaEstadosOT.accionesDisponibles(estado(id), Rol.TECNICO).toSet())
    }

    @Test
    fun noSeEditaEnEjecucionCerradaNiConforme() = runBlocking {
        listOf(EstadoOT.EN_EJECUCION, EstadoOT.CERRADA, EstadoOT.CONFORME).forEach { e ->
            val id = otEn(e)
            assertNotNull("$e no se debe poder editar", repo.actualizar(ot(id).copy(tecnicoId = matias)))
            assertEquals(e, estado(id))
        }
    }

    @Test
    fun cambiarDeLocal_elClienteDejaDeVerla() = runBlocking {
        val id = otEn(EstadoOT.ASIGNADA)
        assertTrue(ve(cliente, Rol.CLIENTE, id))
        assertNull(repo.actualizar(ot(id).copy(localId = 1, equipoId = 1)))
        assertFalse("Ya no es de su local", ve(cliente, Rol.CLIENTE, id))
    }

    // ---- Cancelar desde cada estado ----

    @Test
    fun cancelar_soloAntesDeLaEjecucion() = runBlocking {
        val permitidos = listOf(EstadoOT.CREADA, EstadoOT.ASIGNADA, EstadoOT.ACEPTADA, EstadoOT.RECHAZADA,
            EstadoOT.VISITA_FALLIDA, EstadoOT.POSTERGADA)
        permitidos.forEach { e ->
            val id = otEn(e)
            assertNull("Desde $e se debe poder cancelar", repo.aplicar(id, AccionOT.CANCELAR, Rol.JEFE, jefe, "OT duplicada"))
            assertEquals(EstadoOT.CANCELADA, estado(id))
        }
        listOf(EstadoOT.EN_EJECUCION, EstadoOT.CERRADA, EstadoOT.CONFORME, EstadoOT.CANCELADA).forEach { e ->
            val id = otEn(e)
            assertNotNull("Desde $e NO se debe poder cancelar", repo.aplicar(id, AccionOT.CANCELAR, Rol.JEFE, jefe, "OT duplicada"))
            assertEquals(e, estado(id))
        }
    }

    @Test
    fun motivoInventado_noSeAcepta() = runBlocking {
        val id = otEn(EstadoOT.ASIGNADA)
        assertEquals("Motivo no válido", repo.aplicar(id, AccionOT.CANCELAR, Rol.JEFE, jefe, "Me aburrí"))
        assertEquals(EstadoOT.ASIGNADA, estado(id))
    }

    // ---- Acciones repetidas y de otra persona ----

    @Test
    fun aceptarDosVeces_laSegundaFallaYNoEnsuciaElHistorial() = runBlocking {
        val id = otEn(EstadoOT.ASIGNADA)
        assertNull(repo.aplicar(id, AccionOT.ACEPTAR, Rol.TECNICO, pedro))
        assertEquals("Acción no permitida: la OT está aceptada", repo.aplicar(id, AccionOT.ACEPTAR, Rol.TECNICO, pedro))
        assertEquals(1, historial(id).count { it.startsWith("Aceptada") })
    }

    @Test
    fun checkInDeOtroTecnico_noSePermite() = runBlocking {
        val id = otEn(EstadoOT.ACEPTADA)
        assertEquals("Esta OT no está asignada a ti", repo.checkIn(id, matias, -33.5, -70.7))
        assertEquals(EstadoOT.ACEPTADA, estado(id))
        assertNull(ot(id).checkInFechaHora)
    }

    @Test
    fun cerrarDosVeces_laSegundaFalla() = runBlocking {
        val id = otEn(EstadoOT.CERRADA)
        assertNotNull(repo.cerrar(id, pedro, "Otra vez el diagnóstico", "Otra vez las actividades", "", "content://f2"))
        assertEquals(EstadoOT.CERRADA, estado(id))
    }

    // ---- Revisión de la jefatura en varias vueltas ----

    @Test
    fun dosDisconformidades_yLuegoConformidad() = runBlocking {
        val id = otEn(EstadoOT.CERRADA)
        repeat(2) { vuelta ->
            assertNull(repo.aplicar(id, AccionOT.DISCONFORMIDAD, Rol.JEFE, jefe, "Foto poco clara"))
            assertEquals(EstadoOT.EN_EJECUCION, estado(id))
            assertNull(repo.cerrar(id, pedro, "Corrección número ${vuelta + 1}", "Se repitió la foto", "", "content://f$vuelta"))
            assertEquals(EstadoOT.CERRADA, estado(id))
        }
        assertNull(repo.aplicar(id, AccionOT.CONFORMIDAD, Rol.JEFE, jefe))
        assertEquals(EstadoOT.CONFORME, estado(id))
        assertEquals(2, historial(id).count { it.startsWith("Disconformidad") })
        assertEquals("Conformidad de la jefatura", historial(id).last())
    }

    @Test
    fun visitaFallida_luegoCancelada() = runBlocking {
        val id = otEn(EstadoOT.VISITA_FALLIDA)
        assertNull(repo.aplicar(id, AccionOT.CANCELAR, Rol.JEFE, jefe, "El cliente ya no la necesita"))
        assertEquals(EstadoOT.CANCELADA, estado(id))
        assertEquals("Cancelada: El cliente ya no la necesita", historial(id).last())
    }

    @Test
    fun postergadaAntesDeAceptar_reprogramadaVuelveAAsignada() = runBlocking {
        val id = otEn(EstadoOT.POSTERGADA)
        assertNull(repo.actualizar(ot(id).copy(fechaProgramada = System.currentTimeMillis() + 86_400_000)))
        assertEquals(EstadoOT.ASIGNADA, estado(id))
        assertEquals("Reprogramada para Pedro Soto", historial(id).last())
    }

    // ---- Equipos ----

    @Test
    fun equipoConOtEnCurso_noSePuedeBorrar_yConOtTerminadasSeDaDeBaja() = runBlocking {
        // Equipo nuevo con una sola OT abierta (el Trane de ejemplo ya tiene otra OT abierta)
        val equipoId = equipos.crear(
            Equipo(localId = localCliente, tipo = TipoEquipo.REFRIGERACION, marca = "Bitzer", modelo = "CF-300",
                ubicacion = UbicacionEquipo.INTERIOR)
        )
        val id = crearOt(pedro, equipo = equipoId)
        assertEquals(ResultadoBorrado.TIENE_OT_EN_CURSO, equipos.eliminarODarDeBaja(db.equipoDao().getById(equipoId)!!))
        assertFalse("Sigue activo", db.equipoDao().getById(equipoId)!!.dadoDeBaja)

        // Al cancelar la OT ya no hay nada en curso: se da de baja (tiene historial, no se borra)
        repo.aplicar(id, AccionOT.CANCELAR, Rol.JEFE, jefe, "OT duplicada")
        assertEquals(ResultadoBorrado.DADO_DE_BAJA, equipos.eliminarODarDeBaja(db.equipoDao().getById(equipoId)!!))
        assertTrue(db.equipoDao().getById(equipoId)!!.dadoDeBaja)
        assertFalse("Ya no aparece en el local", equipos.getByLocal(localCliente).first().any { it.id == equipoId })
    }

    @Test
    fun equipoDeEjemploConOtAbierta_noSePuedeBorrar() = runBlocking {
        // El Trane (equipo 4) tiene OT abiertas en los datos de ejemplo
        assertEquals(ResultadoBorrado.TIENE_OT_EN_CURSO, equipos.eliminarODarDeBaja(db.equipoDao().getById(4)!!))
    }

    @Test
    fun equipoSinOt_seElimina() = runBlocking {
        val nuevo = equipos.crear(
            Equipo(localId = localCliente, tipo = TipoEquipo.CLIMATIZACION, marca = "Carrier", modelo = "Split 42",
                ubicacion = UbicacionEquipo.INTERIOR)
        )
        assertEquals(ResultadoBorrado.ELIMINADO, equipos.eliminarODarDeBaja(db.equipoDao().getById(nuevo)!!))
        assertNull(db.equipoDao().getById(nuevo))
    }
}
