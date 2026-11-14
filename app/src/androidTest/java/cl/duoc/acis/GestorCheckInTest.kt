package cl.duoc.acis

import android.location.Location
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cl.duoc.acis.data.AppDatabase
import cl.duoc.acis.data.DatosEjemplo
import cl.duoc.acis.model.AccionOT
import cl.duoc.acis.model.EstadoOT
import cl.duoc.acis.model.Rol
import cl.duoc.acis.repository.OrdenTrabajoRepository
import cl.duoc.acis.util.FuenteUbicacion
import cl.duoc.acis.util.GestorCheckIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * El check-in corre en segundo plano: se inicia, la llamada vuelve al instante y el trabajo
 * termina solo (aunque nadie esté mirando la pantalla). Se usa un GPS falso que demora 1 segundo.
 */
@RunWith(AndroidJUnit4::class)
class GestorCheckInTest {

    private val pedro = 1L

    /** GPS falso: responde después de una espera, o no responde (null). */
    private class GpsFalso(val activado: Boolean = true, val responde: Boolean = true) : FuenteUbicacion {
        var llamadas = 0
        override fun estaActivada() = activado
        override suspend fun obtener(): Location? {
            llamadas++
            delay(1000) // simula lo que tarda el GPS real
            return if (responde) Location("prueba").apply { latitude = -33.511; longitude = -70.7575 } else null
        }
    }

    private lateinit var db: AppDatabase
    private lateinit var repo: OrdenTrabajoRepository

    @Before
    fun crearBase() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        DatosEjemplo.cargar(db)
        repo = OrdenTrabajoRepository(db.ordenTrabajoDao(), db.equipoDao(), db.eventoDao(), db.usuarioDao())
    }

    @After
    fun cerrarBase() = db.close()

    private fun gestor(gps: FuenteUbicacion) =
        GestorCheckIn(repo, gps, CoroutineScope(SupervisorJob() + Dispatchers.Default))

    /** OT de ejemplo 1 (de Pedro) aceptada, lista para el check-in. */
    private suspend fun otAceptada(): Long {
        repo.aplicar(1, AccionOT.ACEPTAR, Rol.TECNICO, pedro)
        return 1
    }

    private suspend fun esperarQueTermine(g: GestorCheckIn, otId: Long) =
        withTimeout(10_000) { while (otId in g.enCurso.value) delay(50) }

    @Test
    fun checkIn_noBloquea_yTerminaSoloEnSegundoPlano() = runBlocking {
        val id = otAceptada()
        val g = gestor(GpsFalso())

        // Iniciar vuelve al instante, aunque el GPS demore 1 segundo
        val inicio = System.currentTimeMillis()
        assertNull(g.iniciar(id, pedro))
        assertTrue("No debe bloquear", System.currentTimeMillis() - inicio < 500)
        assertTrue("Queda marcado como en curso", id in g.enCurso.value)
        assertEquals("Todavía no cambia", EstadoOT.ACEPTADA, repo.getOt(id).first()!!.estado)

        // Nadie mira la pantalla (el técnico se fue a la lista): igual termina
        esperarQueTermine(g, id)
        val ot = repo.getOt(id).first()!!
        assertEquals(EstadoOT.EN_EJECUCION, ot.estado)
        assertEquals(-33.511, ot.checkInLat!!, 0.0001)
        assertEquals("Check-in registrado", g.resultados.value[id])
    }

    @Test
    fun gpsApagado_avisaAlInstanteYNoCambiaNada() = runBlocking {
        val id = otAceptada()
        val gps = GpsFalso(activado = false)
        assertEquals("Activa la ubicación del teléfono para hacer check-in", gestor(gps).iniciar(id, pedro))
        assertEquals(0, gps.llamadas)
        assertEquals(EstadoOT.ACEPTADA, repo.getOt(id).first()!!.estado)
    }

    @Test
    fun sinSenal_avisaYLaOtSigueAceptada() = runBlocking {
        val id = otAceptada()
        val g = gestor(GpsFalso(responde = false))
        g.iniciar(id, pedro)
        esperarQueTermine(g, id)
        assertEquals(EstadoOT.ACEPTADA, repo.getOt(id).first()!!.estado)
        assertTrue(g.resultados.value[id]!!.startsWith("No se pudo obtener la ubicación"))
    }

    @Test
    fun tocarDosVeces_haceUnSoloCheckIn() = runBlocking {
        val id = otAceptada()
        val gps = GpsFalso()
        val g = gestor(gps)
        g.iniciar(id, pedro)
        g.iniciar(id, pedro) // segundo toque mientras busca la ubicación
        esperarQueTermine(g, id)
        assertEquals("El GPS se consulta una sola vez", 1, gps.llamadas)
        assertEquals(1, repo.getHistorial(id).first().count { it.descripcion.startsWith("Check-in") })
    }

    @Test
    fun siLaOtCambiaMientrasBuscaElGps_noSeGuardaElCheckIn() = runBlocking {
        val id = otAceptada()
        val g = gestor(GpsFalso())
        g.iniciar(id, pedro)
        // Mientras el GPS busca, el jefe la cancela
        repo.aplicar(id, AccionOT.CANCELAR, Rol.JEFE, 2, "OT duplicada")
        esperarQueTermine(g, id)
        assertEquals("La máquina de estados lo impide", EstadoOT.CANCELADA, repo.getOt(id).first()!!.estado)
        assertEquals("Acción no permitida: la OT está cancelada", g.resultados.value[id])
    }
}
