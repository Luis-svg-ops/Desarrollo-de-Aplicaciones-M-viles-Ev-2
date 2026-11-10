package cl.duoc.acis

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cl.duoc.acis.data.AppDatabase
import cl.duoc.acis.data.DatosEjemplo
import cl.duoc.acis.model.AccionOT
import cl.duoc.acis.model.EstadoOT
import cl.duoc.acis.model.MaquinaEstadosOT
import cl.duoc.acis.model.OrdenTrabajo
import cl.duoc.acis.model.Prioridad
import cl.duoc.acis.model.Rol
import cl.duoc.acis.model.TipoOT
import cl.duoc.acis.repository.OrdenTrabajoRepository
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
 * Recorre todas las rutas de una OT con la base de datos real (Room en memoria, con los datos de ejemplo)
 * y revisa en cada paso: el estado, quién ve la OT y qué botones tiene cada rol.
 * Corre en el emulador: ./gradlew connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class RutasOtTest {

    // Usuarios de los datos de ejemplo
    private val pedro = 1L     // técnico
    private val jefe = 2L      // Carolina Muñoz
    private val matias = 3L    // otro técnico
    private val cliente = 6L   // Andrea Pino, administra el local 2 (Mall Costanera)
    private val localCliente = 2L

    private lateinit var db: AppDatabase
    private lateinit var repo: OrdenTrabajoRepository

    @Before
    fun crearBase() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        DatosEjemplo.cargar(db)
        repo = OrdenTrabajoRepository(db.ordenTrabajoDao(), db.equipoDao())
    }

    @After
    fun cerrarBase() = db.close()

    // ---- Ayudas ----

    /** El jefe crea una OT en el local del cliente (equipo 4: Trane del Mall Costanera). */
    private suspend fun crearOt(tecnico: Long?): Long = repo.crear(
        OrdenTrabajo(
            numero = "", localId = localCliente, equipoId = 4, tecnicoId = tecnico,
            tipo = TipoOT.CORRECTIVA, prioridad = Prioridad.ALTA,
            estado = MaquinaEstadosOT.estadoInicial(tecnico),
            descripcion = "Equipo hace ruido al partir", fechaCreacion = System.currentTimeMillis(),
            fechaProgramada = System.currentTimeMillis() + 3_600_000
        )
    )

    private suspend fun ot(id: Long) = repo.getOt(id).first()!!
    private suspend fun estado(id: Long) = ot(id).estado

    /** ¿Aparece la OT en la lista de este usuario? (lo mismo que usa la pantalla de lista) */
    private suspend fun ve(usuario: Long, rol: Rol, id: Long): Boolean =
        repo.getParaUsuario(usuario, rol, if (rol == Rol.CLIENTE) localCliente else null).first().any { it.ot.id == id }

    /** Botones que la pantalla de detalle le muestra a ese rol. */
    private suspend fun botones(rol: Rol, id: Long) =
        MaquinaEstadosOT.accionesDisponibles(estado(id), rol).filter { !it.enFormulario }.toSet()

    /** El jefe guarda el formulario de edición cambiando el técnico. */
    private suspend fun jefeAsigna(id: Long, tecnico: Long?) = repo.actualizar(ot(id).copy(tecnicoId = tecnico))

    // ---- Ruta 1: camino principal completo ----

    @Test
    fun caminoPrincipal_jefeAsigna_tecnicoEjecuta_jefeRevisa_clienteSigue() = runBlocking {
        val id = crearOt(tecnico = pedro)

        // 1. Asignada: le aparece a Pedro y al cliente, no a otro técnico
        assertEquals(EstadoOT.ASIGNADA, estado(id))
        assertTrue("Pedro debe ver la OT asignada", ve(pedro, Rol.TECNICO, id))
        assertFalse("Matías no debe verla", ve(matias, Rol.TECNICO, id))
        assertTrue("El cliente debe verla", ve(cliente, Rol.CLIENTE, id))
        assertEquals(setOf(AccionOT.ACEPTAR, AccionOT.RECHAZAR), botones(Rol.TECNICO, id))
        assertEquals(setOf(AccionOT.POSTERGAR, AccionOT.CANCELAR), botones(Rol.JEFE, id))
        assertTrue("El cliente no tiene botones", botones(Rol.CLIENTE, id).isEmpty())

        // 2. Pedro acepta
        assertNull(repo.aplicar(id, AccionOT.ACEPTAR, Rol.TECNICO, pedro))
        assertEquals(EstadoOT.ACEPTADA, estado(id))
        assertNotNull("Se guarda cuándo aceptó", ot(id).fechaAceptacion)

        // 3. Check-in con GPS
        assertNull(repo.checkIn(id, pedro, -33.511, -70.7575))
        assertEquals(EstadoOT.EN_EJECUCION, estado(id))
        assertNotNull(ot(id).checkInFechaHora)

        // 4. Diagnóstico, foto y check-out
        val antes = System.currentTimeMillis()
        assertNull(repo.cerrar(id, pedro, "Rodamiento gastado", "Cambio de rodamiento", "", "content://foto1"))
        assertEquals(EstadoOT.CERRADA, estado(id))
        val equipo = db.equipoDao().getById(4)!!
        assertTrue("La última mantención del equipo pasa a hoy", equipo.fechaUltimaMantencion!! >= antes)
        assertTrue("El cliente sigue viendo la OT cerrada", ve(cliente, Rol.CLIENTE, id))

        // 5. El jefe revisa: el técnico no puede dar conformidad
        assertEquals(setOf(AccionOT.CONFORMIDAD, AccionOT.DISCONFORMIDAD), botones(Rol.JEFE, id))
        assertTrue(botones(Rol.TECNICO, id).isEmpty())
        assertNotNull(repo.aplicar(id, AccionOT.CONFORMIDAD, Rol.TECNICO, pedro))

        // 6. Disconformidad: sin motivo no se puede; con motivo vuelve a Pedro
        assertEquals("Selecciona un motivo", repo.aplicar(id, AccionOT.DISCONFORMIDAD, Rol.JEFE, jefe))
        assertNull(repo.aplicar(id, AccionOT.DISCONFORMIDAD, Rol.JEFE, jefe, "Trabajo incompleto"))
        assertEquals(EstadoOT.EN_EJECUCION, estado(id))
        assertEquals("Trabajo incompleto", ot(id).motivo)
        assertTrue("Pedro la vuelve a ver para corregir", ve(pedro, Rol.TECNICO, id))
        assertEquals(setOf(AccionOT.CERRAR), botones(Rol.TECNICO, id))

        // 7. Pedro corrige y cierra de nuevo: el motivo se borra
        assertNull(repo.cerrar(id, pedro, "Rodamiento y correa cambiados", "Cambio completo", "Correa", "content://foto2"))
        assertEquals(EstadoOT.CERRADA, estado(id))
        assertNull(ot(id).motivo)

        // 8. Conformidad: fin del ciclo, nadie tiene botones y no se edita
        assertNull(repo.aplicar(id, AccionOT.CONFORMIDAD, Rol.JEFE, jefe))
        assertEquals(EstadoOT.CONFORME, estado(id))
        Rol.entries.forEach { assertTrue("$it no debe tener botones", botones(it, id).isEmpty()) }
        assertFalse(MaquinaEstadosOT.sePuedeEditar(estado(id)))
        assertTrue("El cliente ve el resultado final", ve(cliente, Rol.CLIENTE, id))
    }

    // ---- Ruta 2: el técnico rechaza y el jefe reasigna ----

    @Test
    fun rechazo_jefeReasignaAOtroTecnico() = runBlocking {
        val id = crearOt(tecnico = pedro)
        assertEquals("Selecciona un motivo", repo.aplicar(id, AccionOT.RECHAZAR, Rol.TECNICO, pedro))
        assertNull(repo.aplicar(id, AccionOT.RECHAZAR, Rol.TECNICO, pedro, "Fuera de mi zona"))
        assertEquals(EstadoOT.RECHAZADA, estado(id))
        assertTrue("Pedro no tiene más botones", botones(Rol.TECNICO, id).isEmpty())

        // El jefe se la asigna a Matías: vuelve a Asignada, sin el motivo anterior
        assertNull(jefeAsigna(id, matias))
        assertEquals(EstadoOT.ASIGNADA, estado(id))
        assertNull(ot(id).motivo)
        assertTrue("Matías ahora la ve", ve(matias, Rol.TECNICO, id))
        assertFalse("Pedro ya no la ve", ve(pedro, Rol.TECNICO, id))
        assertNotNull("Pedro ya no puede aceptarla", repo.aplicar(id, AccionOT.ACEPTAR, Rol.TECNICO, pedro))
        assertNull(repo.aplicar(id, AccionOT.ACEPTAR, Rol.TECNICO, matias))
    }

    // ---- Ruta 3: visita fallida y reprogramación ----

    @Test
    fun visitaFallida_jefeReprograma_tecnicoDebeAceptarDeNuevo() = runBlocking {
        val id = crearOt(tecnico = pedro)
        assertNotNull("No hay visita fallida sin aceptar antes",
            repo.aplicar(id, AccionOT.VISITA_FALLIDA, Rol.TECNICO, pedro, "Local cerrado"))
        repo.aplicar(id, AccionOT.ACEPTAR, Rol.TECNICO, pedro)
        assertNull(repo.aplicar(id, AccionOT.VISITA_FALLIDA, Rol.TECNICO, pedro, "Local cerrado"))
        assertEquals(EstadoOT.VISITA_FALLIDA, estado(id))

        // Reprograma con el mismo técnico: vuelve a Asignada y Pedro tiene que aceptarla otra vez
        assertNull(repo.actualizar(ot(id).copy(fechaProgramada = System.currentTimeMillis() + 86_400_000)))
        assertEquals(EstadoOT.ASIGNADA, estado(id))
        assertNull(ot(id).fechaAceptacion)
        assertEquals(setOf(AccionOT.ACEPTAR, AccionOT.RECHAZAR), botones(Rol.TECNICO, id))
    }

    // ---- Ruta 4: postergar y cancelar ----

    @Test
    fun postergar_luegoCancelar_quedaSinSalida() = runBlocking {
        val id = crearOt(tecnico = pedro)
        repo.aplicar(id, AccionOT.ACEPTAR, Rol.TECNICO, pedro)
        assertNotNull("El técnico no posterga", repo.aplicar(id, AccionOT.POSTERGAR, Rol.TECNICO, pedro, "Solicitud del cliente"))
        assertNull(repo.aplicar(id, AccionOT.POSTERGAR, Rol.JEFE, jefe, "Solicitud del cliente"))
        assertEquals(EstadoOT.POSTERGADA, estado(id))
        assertEquals(setOf(AccionOT.CANCELAR), botones(Rol.JEFE, id))

        assertNull(repo.aplicar(id, AccionOT.CANCELAR, Rol.JEFE, jefe, "El cliente ya no la necesita"))
        assertEquals(EstadoOT.CANCELADA, estado(id))
        Rol.entries.forEach { assertTrue(botones(it, id).isEmpty()) }
        assertNotNull("Una OT cancelada no se acepta", repo.aplicar(id, AccionOT.ACEPTAR, Rol.TECNICO, pedro))
        assertNotNull("Una OT cancelada no se edita", repo.actualizar(ot(id).copy(tecnicoId = matias)))
    }

    // ---- Ruta 5: OT sin técnico ----

    @Test
    fun sinTecnico_soloLaVeElJefeHastaQueAsigna() = runBlocking {
        val id = crearOt(tecnico = null)
        assertEquals(EstadoOT.CREADA, estado(id))
        assertFalse("Ningún técnico la ve", ve(pedro, Rol.TECNICO, id) || ve(matias, Rol.TECNICO, id))
        assertTrue("El jefe sí", ve(jefe, Rol.JEFE, id))
        assertTrue("El cliente ve que su solicitud existe", ve(cliente, Rol.CLIENTE, id))

        assertNull(jefeAsigna(id, pedro))
        assertEquals(EstadoOT.ASIGNADA, estado(id))
        assertTrue("Al asignar, a Pedro le aparece", ve(pedro, Rol.TECNICO, id))
    }

    // ---- Ruta 6: lo que nadie puede hacer ----

    @Test
    fun accionesProhibidas() = runBlocking {
        val id = crearOt(tecnico = pedro)
        assertEquals("Esta OT no está asignada a ti", repo.aplicar(id, AccionOT.ACEPTAR, Rol.TECNICO, matias))
        assertNotNull("El cliente no cancela", repo.aplicar(id, AccionOT.CANCELAR, Rol.CLIENTE, cliente, "OT duplicada"))
        assertNotNull("No hay check-in sin aceptar", repo.checkIn(id, pedro, 0.0, 0.0))
        assertNotNull("No se cierra sin check-in",
            repo.cerrar(id, pedro, "Diagnóstico de prueba", "Actividad de prueba", "", "content://x"))
        assertEquals("Nada cambió", EstadoOT.ASIGNADA, estado(id))
    }

    // ---- Ruta 7: el cliente solo ve su local ----

    @Test
    fun clienteSoloVeSuLocal() = runBlocking {
        val delCliente = repo.getParaUsuario(cliente, Rol.CLIENTE, localCliente).first()
        assertTrue(delCliente.isNotEmpty())
        assertTrue("Todas son del Mall Costanera", delCliente.all { it.ot.localId == localCliente })
    }
}
