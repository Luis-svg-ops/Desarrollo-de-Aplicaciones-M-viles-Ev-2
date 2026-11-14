package cl.duoc.acis.aceptacion

import cl.duoc.acis.util.nombreArchivoInforme
import cl.duoc.acis.util.guardarTexto
import cl.duoc.acis.util.crearInforme
import cl.duoc.acis.util.InformeJson
import android.net.Uri
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
import cl.duoc.acis.model.UbicacionEquipo
import cl.duoc.acis.repository.EquipoRepository
import cl.duoc.acis.repository.OrdenTrabajoRepository
import cl.duoc.acis.repository.ResultadoBorrado
import cl.duoc.acis.repository.UsuarioRepository
import cl.duoc.acis.util.crearUriFoto
import cl.duoc.acis.util.esHoy
import cl.duoc.acis.util.validarEmail
import cl.duoc.acis.view.components.textoEstado
import cl.duoc.acis.viewmodel.ListaOtViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Pruebas de aceptación: cada prueba es un escenario de una historia de usuario del Excel
 * (hoja "Historias de Usuario"), escrito como Dado / Cuando / Entonces (Given / When / Then).
 * Es lo mismo que Arrange / Act / Assert, pero contado como lo vive el usuario.
 * La hoja "Trazabilidad" del Excel indica qué prueba cubre cada escenario.
 *
 * Corren en el emulador con la base real de Room (en memoria) y los datos de ejemplo.
 */
@RunWith(AndroidJUnit4::class)
class CriteriosAceptacionTest {

    // Usuarios de los datos de ejemplo
    private val pedro = 1L      // técnico
    private val jefe = 2L       // Carolina Muñoz
    private val matias = 3L     // otro técnico
    private val cliente = 6L    // Andrea Pino, Mall Costanera (local 2)
    private val localCliente = 2L

    private lateinit var db: AppDatabase
    private lateinit var ots: OrdenTrabajoRepository
    private lateinit var equipos: EquipoRepository
    private lateinit var usuarios: UsuarioRepository

    @Before
    fun crearBase() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        DatosEjemplo.cargar(db)
        ots = OrdenTrabajoRepository(db.ordenTrabajoDao(), db.equipoDao(), db.eventoDao(), db.usuarioDao())
        equipos = EquipoRepository(db.equipoDao(), db.ordenTrabajoDao())
        usuarios = UsuarioRepository(db.usuarioDao())
    }

    @After
    fun cerrarBase() = db.close()

    // ---- Ayudas ----

    private suspend fun nuevaOt(tecnico: Long?, prioridad: Prioridad = Prioridad.MEDIA, equipo: Long = 4): Long = ots.crear(
        OrdenTrabajo(
            numero = "", localId = localCliente, equipoId = equipo, tecnicoId = tecnico,
            tipo = TipoOT.CORRECTIVA, prioridad = prioridad, estado = MaquinaEstadosOT.estadoInicial(tecnico),
            descripcion = "Equipo con ruido al partir", fechaCreacion = System.currentTimeMillis(),
            fechaProgramada = System.currentTimeMillis() + 3_600_000
        )
    )

    private suspend fun ot(id: Long) = ots.getOt(id).first()!!
    private suspend fun historial(id: Long) = ots.getHistorial(id).first().map { it.descripcion }
    private suspend fun lista(usuario: Long, rol: Rol, localId: Long? = null) =
        ots.getParaUsuario(usuario, rol, localId).first()

    private suspend fun cerrada(id: Long) {
        ots.aplicar(id, AccionOT.ACEPTAR, Rol.TECNICO, pedro)
        ots.checkIn(id, pedro, -33.511, -70.7575)
        ots.cerrar(id, pedro, "Rodamiento gastado", "Cambio de rodamiento", "", "content://foto")
    }

    /** Espera a que el StateFlow de un ViewModel cumpla la condición (como lo vería la pantalla). */
    private suspend fun <T> StateFlow<T>.esperar(condicion: (T) -> Boolean): T =
        withTimeout(5_000) { first(condicion) }

    // ================= HU-01 Iniciar sesión según rol =================

    @Test
    fun hu01_inicioDeSesionExitoso_cadaRolEntraConSuRol() = runBlocking {
        // Dado que tengo una cuenta activa (técnico, jefe o cliente)
        // Cuando ingreso mi correo y contraseña correctos
        val tecnico = usuarios.login("tecnico@acis.cl", "123456")
        val jefatura = usuarios.login("  JEFE@acis.cl ", "123456") // con espacios y mayúsculas
        val clienteLocal = usuarios.login("cliente@acis.cl", "123456")
        // Entonces entro con mi rol
        assertEquals(Rol.TECNICO, tecnico?.rol)
        assertEquals(Rol.JEFE, jefatura?.rol)
        assertEquals(Rol.CLIENTE, clienteLocal?.rol)
    }

    @Test
    fun hu01_credencialesIncorrectas_noAccedo() = runBlocking {
        // Dado que ingreso una contraseña incorrecta
        // Cuando presiono "Ingresar"
        val resultado = usuarios.login("tecnico@acis.cl", "000000")
        // Entonces no accedo a la aplicación (la pantalla muestra "Correo o contraseña incorrectos")
        assertNull(resultado)
        // Y un correo mal escrito se avisa antes de intentar
        assertEquals("Formato de correo no válido (ej.: nombre@acis.cl)", validarEmail("tecnicoacis.cl"))
    }

    // ================= HU-07 Crear orden de trabajo =================

    @Test
    fun hu07_crearOt_quedaCreadaConNumeroUnicoYFecha() = runBlocking {
        // Dado que estoy en "Nueva OT" y completo los datos, sin técnico
        val antes = System.currentTimeMillis()
        // Cuando presiono "Crear"
        val id = nuevaOt(tecnico = null)
        val otra = nuevaOt(tecnico = null)
        // Entonces la OT queda "Creada" con un número único
        assertEquals(EstadoOT.CREADA, ot(id).estado)
        assertTrue(Regex("OT-\\d{4}-\\d{4}").matches(ot(id).numero))
        assertFalse("El número no se repite", ot(id).numero == ot(otra).numero)
        // Y se registra la fecha y hora de creación
        assertTrue(ot(id).fechaCreacion >= antes)
    }

    // ================= HU-09 Ver mi agenda del día =================

    @Test
    fun hu09_verAgenda_veoMisOtYLaProximaVisita() = runBlocking {
        // Dado que tengo OT asignadas para hoy
        val vm = ListaOtViewModel(ots, pedro, Rol.TECNICO)
        // Cuando abro la aplicación (pestaña "Pendientes")
        val pendientes = vm.otsFiltradas.esperar { it.isNotEmpty() }
        // Entonces veo solo mis OT, con local y prioridad
        assertTrue(pendientes.all { it.ot.tecnicoId == pedro && it.local.direccion.isNotBlank() })
        // Y la "Próxima visita" es la más cercana de hoy
        val esperada = pendientes.filter { esHoy(it.ot.fechaProgramada) }.minByOrNull { it.ot.fechaProgramada }
        assertEquals(esperada?.ot?.id, vm.proximaVisita.esperar { it != null }?.ot?.id)
    }

    // ================= HU-11 Aceptar o rechazar OT =================

    @Test
    fun hu11_aceptarOt_pasaAAceptada() = runBlocking {
        // Dado que tengo una OT "Asignada"
        val id = nuevaOt(tecnico = pedro)
        // Cuando presiono "Aceptar"
        val error = ots.aplicar(id, AccionOT.ACEPTAR, Rol.TECNICO, pedro)
        // Entonces la OT pasa a "Aceptada"
        assertNull(error)
        assertEquals(EstadoOT.ACEPTADA, ot(id).estado)
    }

    @Test
    fun hu11_rechazarSinMotivo_noSePuedeConfirmar() = runBlocking {
        // Dado que presiono "Rechazar"
        val id = nuevaOt(tecnico = pedro)
        // Cuando no elijo un motivo
        val error = ots.aplicar(id, AccionOT.RECHAZAR, Rol.TECNICO, pedro, motivo = null)
        // Entonces la app no me deja confirmar el rechazo
        assertEquals("Selecciona un motivo", error)
        assertEquals(EstadoOT.ASIGNADA, ot(id).estado)
    }

    @Test
    fun hu11_otYaReasignada_noRegistraMiAceptacion() = runBlocking {
        // Dado que el jefe reasignó la OT a otro técnico
        val id = nuevaOt(tecnico = pedro)
        ots.actualizar(ot(id).copy(tecnicoId = matias))
        // Cuando intento aceptarla
        val error = ots.aplicar(id, AccionOT.ACEPTAR, Rol.TECNICO, pedro)
        // Entonces la app me informa que la asignación cambió y no registra mi aceptación
        assertEquals("Esta OT no está asignada a ti", error)
        assertEquals(EstadoOT.ASIGNADA, ot(id).estado)
        assertEquals(matias, ot(id).tecnicoId)
    }

    // ================= HU-12 Reprogramar o reasignar OT =================

    @Test
    fun hu12_reasignarOtRechazada_apareceAlNuevoYQuedaEnHistorial() = runBlocking {
        // Dado que un técnico rechazó una OT
        val id = nuevaOt(tecnico = pedro)
        ots.aplicar(id, AccionOT.RECHAZAR, Rol.TECNICO, pedro, "Fuera de mi zona")
        // Cuando la asigno a otro técnico
        assertNull(ots.actualizar(ot(id).copy(tecnicoId = matias)))
        // Entonces la OT aparece en la agenda del nuevo técnico
        assertTrue(lista(matias, Rol.TECNICO).any { it.ot.id == id })
        // Y el cambio queda registrado en el historial de la OT
        assertEquals("Asignada a Matías Rojas", historial(id).last())
    }

    // ================= HU-13 Hacer check-in en el local =================

    @Test
    fun hu13_checkIn_guardaHoraUbicacionYPasaAEjecucion() = runBlocking {
        // Dado que tengo una OT "Aceptada"
        val id = nuevaOt(tecnico = pedro)
        ots.aplicar(id, AccionOT.ACEPTAR, Rol.TECNICO, pedro)
        val antes = System.currentTimeMillis()
        // Cuando presiono "Check-in" (con permiso de ubicación)
        assertNull(ots.checkIn(id, pedro, -33.511, -70.7575))
        // Entonces se guarda la fecha y hora y la ubicación, y la OT pasa a "En ejecución"
        val o = ot(id)
        assertTrue(o.checkInFechaHora!! >= antes)
        assertEquals(-33.511, o.checkInLat!!, 0.0001)
        assertEquals(EstadoOT.EN_EJECUCION, o.estado)
    }

    // ================= HU-24 Consultar estado de mis OT (cliente) =================

    @Test
    fun hu24_clienteVeSoloLasOtDeSuLocal() = runBlocking {
        // Dado que inicié sesión como cliente
        val otraDeOtroLocal = ots.crear(ot(nuevaOt(pedro)).copy(id = 0, localId = 1, equipoId = 1))
        // Cuando abro "Mis OT"
        val mias = lista(cliente, Rol.CLIENTE, localCliente)
        // Entonces veo solo las OT de mi local, con su estado, y no las de otros
        assertTrue(mias.isNotEmpty())
        assertTrue(mias.all { it.ot.localId == localCliente })
        assertFalse(mias.any { it.ot.id == otraDeOtroLocal })
        // Y no tengo botones para modificarlas
        mias.forEach { assertTrue(MaquinaEstadosOT.accionesDisponibles(it.ot.estado, Rol.CLIENTE).isEmpty()) }
    }

    // ================= HU-25 Validar conformidad del trabajo (jefatura) =================

    @Test
    fun hu25_darConformidad_quedaConformeYNadieLaModifica() = runBlocking {
        // Dado que una OT está "Cerrada" con diagnóstico, actividades y foto
        val id = nuevaOt(tecnico = pedro); cerrada(id)
        // Cuando reviso la evidencia y presiono "Dar conformidad"
        assertNull(ots.aplicar(id, AccionOT.CONFORMIDAD, Rol.JEFE, jefe))
        // Entonces la OT queda "Conforme" y ya nadie puede modificarla
        assertEquals(EstadoOT.CONFORME, ot(id).estado)
        Rol.entries.forEach { assertTrue(MaquinaEstadosOT.accionesDisponibles(EstadoOT.CONFORME, it).isEmpty()) }
        assertNotNull(ots.actualizar(ot(id).copy(prioridad = Prioridad.ALTA)))
    }

    @Test
    fun hu25_disconformidad_vuelveAlTecnicoConElMotivoEnElHistorial() = runBlocking {
        // Dado que la evidencia no es suficiente
        val id = nuevaOt(tecnico = pedro); cerrada(id)
        // Cuando presiono "Disconformidad" y elijo un motivo
        assertNull(ots.aplicar(id, AccionOT.DISCONFORMIDAD, Rol.JEFE, jefe, "Foto poco clara"))
        // Entonces la OT vuelve a "En ejecución" para que el técnico la corrija
        assertEquals(EstadoOT.EN_EJECUCION, ot(id).estado)
        // Y el motivo queda en el historial
        assertEquals("Disconformidad de la jefatura: Foto poco clara", historial(id).last())
    }

    @Test
    fun hu25_disconformidadSinMotivo_noSePuedeConfirmar() = runBlocking {
        // Dado que presiono "Disconformidad"
        val id = nuevaOt(tecnico = pedro); cerrada(id)
        // Cuando no elijo un motivo
        val error = ots.aplicar(id, AccionOT.DISCONFORMIDAD, Rol.JEFE, jefe)
        // Entonces la app no me deja confirmar
        assertEquals("Selecciona un motivo", error)
        assertEquals(EstadoOT.CERRADA, ot(id).estado)
    }

    @Test
    fun hu25_clienteVeEnRevisionYLuegoConforme() = runBlocking {
        // Dado que soy el cliente del local
        val id = nuevaOt(tecnico = pedro); cerrada(id)
        // Cuando la OT está "Cerrada"
        val estadoCerrada = lista(cliente, Rol.CLIENTE, localCliente).first { it.ot.id == id }.ot.estado
        // Entonces la veo "En revisión"
        assertEquals("En revisión", textoEstado(estadoCerrada, paraCliente = true))
        // Y cuando la jefatura da la conformidad la veo "Conforme"
        ots.aplicar(id, AccionOT.CONFORMIDAD, Rol.JEFE, jefe)
        val estadoFinal = lista(cliente, Rol.CLIENTE, localCliente).first { it.ot.id == id }.ot.estado
        assertEquals("Conforme", textoEstado(estadoFinal, paraCliente = true))
    }

    // ================= HU-33 Resguardo de las fotos =================

    @Test
    fun hu33_laFotoSeGuardaEnLaCarpetaPrivadaNoEnLaGaleria() {
        // Dado que el técnico va a tomar una foto de evidencia
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // Cuando la app prepara dónde guardarla
        val uri = crearUriFoto(context)
        // Entonces queda en la carpeta privada de la app (files/fotos), entregada con FileProvider,
        // y no en la galería (que es almacenamiento compartido)
        assertEquals("${context.packageName}.fotos", uri.authority)
        assertTrue(uri.path!!.startsWith("/fotos/"))
        assertTrue(File(context.filesDir, "fotos").isDirectory)
    }

    // ================= HU-36 Editar o dar de baja un equipo =================

    @Test
    fun hu36_darDeBaja_noApareceAlCrearOtYConservaHistorial() = runBlocking {
        // Dado que un equipo fue retirado y sus OT ya terminaron
        val equipoId = equipos.crear(
            cl.duoc.acis.model.Equipo(localId = localCliente, tipo = cl.duoc.acis.model.TipoEquipo.CLIMATIZACION,
                marca = "LG", modelo = "Split 12", ubicacion = UbicacionEquipo.INTERIOR)
        )
        val id = nuevaOt(tecnico = null, equipo = equipoId)
        ots.aplicar(id, AccionOT.CANCELAR, Rol.JEFE, jefe, "OT duplicada")
        // Cuando lo marco como "De baja"
        assertEquals(ResultadoBorrado.DADO_DE_BAJA, equipos.eliminarODarDeBaja(db.equipoDao().getById(equipoId)!!))
        // Entonces ya no aparece al crear nuevas OT
        assertFalse(equipos.getByLocal(localCliente).first().any { it.id == equipoId })
        // Y su historial se conserva
        assertTrue(lista(jefe, Rol.JEFE).any { it.ot.id == id })
    }

    @Test
    fun hu36_equipoConOtAbierta_avisaQuePrimeroHayQueCerrarla() = runBlocking {
        // Dado que el equipo tiene una OT sin cerrar
        val id = nuevaOt(tecnico = pedro)
        // Cuando intento darlo de baja
        val resultado = equipos.eliminarODarDeBaja(db.equipoDao().getById(ot(id).equipoId)!!)
        // Entonces la app me avisa que primero debo cerrar o cancelar esa OT
        assertEquals(ResultadoBorrado.TIENE_OT_EN_CURSO, resultado)
    }

    // ================= HU-39 Ver todas las OT con filtros =================

    @Test
    fun hu39_filtrarEnCurso_veoSoloEsasConTecnicoYLocal() = runBlocking {
        // Dado que existen OT en distintos estados
        val vm = ListaOtViewModel(ots, jefe, Rol.JEFE)
        vm.otsFiltradas.esperar { it.isNotEmpty() }
        // Cuando filtro por "En curso"
        vm.seleccionarFiltro(vm.filtros.indexOfFirst { it.texto == "En curso" })
        val enCurso = vm.otsFiltradas.esperar { l -> l.all { it.ot.estado in setOf(EstadoOT.ASIGNADA, EstadoOT.ACEPTADA, EstadoOT.EN_EJECUCION) } }
        // Entonces veo solo esas OT, cada una con su local
        assertTrue(enCurso.isNotEmpty())
        assertTrue(enCurso.all { it.local.nombre.isNotBlank() })
    }

    @Test
    fun hu39_filtroSinResultados_laListaQuedaVacia() = runBlocking {
        // Dado que aplico un filtro sin resultados (el técnico sin OT en ejecución)
        val vm = ListaOtViewModel(ots, pedro, Rol.TECNICO)
        vm.otsFiltradas.esperar { it.isNotEmpty() }
        // Cuando elijo "En ejecución"
        vm.seleccionarFiltro(vm.filtros.indexOfFirst { it.texto == "En ejecución" })
        // Entonces la lista queda vacía (la pantalla muestra "No hay OT en este filtro")
        assertTrue(vm.otsFiltradas.esperar { it.isEmpty() }.isEmpty())
    }

    // ================= HU-40 Cancelar una OT =================

    @Test
    fun hu40_cancelarConMotivo_desapareceDeLaAgendaDelTecnico() = runBlocking {
        // Dado que una OT no está cerrada
        val id = nuevaOt(tecnico = pedro)
        // Cuando la cancelo y elijo el motivo
        assertNull(ots.aplicar(id, AccionOT.CANCELAR, Rol.JEFE, jefe, "El cliente ya no la necesita"))
        // Entonces la OT pasa a "Cancelada"
        assertEquals(EstadoOT.CANCELADA, ot(id).estado)
        // Y desaparece de la agenda del técnico (sus Pendientes)
        val vm = ListaOtViewModel(ots, pedro, Rol.TECNICO)
        assertFalse(vm.otsFiltradas.esperar { it.isNotEmpty() }.any { it.ot.id == id })
    }

    @Test
    fun hu40_otYaCerrada_noSePuedeCancelar() = runBlocking {
        // Dado que la OT ya está cerrada
        val id = nuevaOt(tecnico = pedro); cerrada(id)
        // Cuando intento cancelarla
        val error = ots.aplicar(id, AccionOT.CANCELAR, Rol.JEFE, jefe, "OT duplicada")
        // Entonces la app no lo permite
        assertNotNull(error)
        assertEquals(EstadoOT.CERRADA, ot(id).estado)
    }

    // ================= HU-50 Consultar el historial de una OT =================

    @Test
    fun hu50_historial_muestraCadaCambioConUsuarioYHora() = runBlocking {
        // Dado que una OT fue creada, asignada, ejecutada y cerrada
        val antes = System.currentTimeMillis()
        val id = nuevaOt(tecnico = pedro); cerrada(id)
        // Cuando abro su historial
        val eventos = ots.getHistorial(id).first()
        // Entonces veo cada cambio de estado con usuario, fecha y hora
        assertEquals(
            listOf("Creada por la jefatura", "Asignada a Pedro Soto", "Aceptada por Pedro Soto",
                "Check-in de Pedro Soto en el local", "Trabajo terminado por Pedro Soto (diagnóstico y foto)"),
            eventos.map { it.descripcion }
        )
        assertTrue(eventos.all { it.fecha >= antes })
    }

    // ================= HU-63 Ver los equipos de un local =================

    @Test
    fun hu63_detalleDelLocal_muestraSusEquiposConTipoMarcaYUbicacion() = runBlocking {
        // Dado que abro el detalle de un local
        // Cuando veo la sección "Equipos"
        val lista = equipos.getByLocal(localCliente).first()
        // Entonces aparecen sus equipos con tipo, marca y si son interiores o exteriores
        assertTrue(lista.isNotEmpty())
        assertTrue(lista.all { it.localId == localCliente && it.marca.isNotBlank() })
        assertTrue(lista.any { it.ubicacion == UbicacionEquipo.EXTERIOR })
    }

    // ================= HU-67 Editar una OT antes de asignarla =================

    @Test
    fun hu67_editarPrioridad_quedaGuardadaYEnElHistorial() = runBlocking {
        // Dado que una OT está "Creada"
        val id = nuevaOt(tecnico = null, prioridad = Prioridad.BAJA)
        // Cuando cambio su prioridad y guardo
        assertNull(ots.actualizar(ot(id).copy(prioridad = Prioridad.ALTA)))
        // Entonces la OT queda con la nueva prioridad
        assertEquals(Prioridad.ALTA, ot(id).prioridad)
        assertEquals(EstadoOT.CREADA, ot(id).estado)
        // Y el cambio queda en su historial
        assertEquals("Datos editados por la jefatura", historial(id).last())
    }

    // ================= HU-26 Descargar informe de la OT (JSON con Gson) =================

    @Test
    fun hu26_exportarInforme_elArchivoTieneLosDatosDeLaOt() = runBlocking<Unit> {
        // Dado que la OT está validada (Conforme)
        val id = nuevaOt(tecnico = pedro); cerrada(id)
        ots.aplicar(id, AccionOT.CONFORMIDAD, Rol.JEFE, jefe)
        val detalle = ots.getDetalle(id).first()!!
        // Cuando presiono "Exportar informe" y elijo dónde guardarlo
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val archivo = File(context.cacheDir, nombreArchivoInforme(detalle.ot.numero))
        val json = InformeJson.aJson(crearInforme(detalle, ots.getHistorial(id).first()))
        assertTrue(guardarTexto(context, Uri.fromFile(archivo), json))
        // Entonces obtengo un archivo con fechas, diagnóstico, actividades e historial
        val leido = InformeJson.desdeJson(archivo.readText())
        assertEquals(detalle.ot.numero, leido.numero)
        assertEquals("Conforme", leido.estado)
        assertEquals("Rodamiento gastado", leido.diagnostico)
        assertEquals("Cambio de rodamiento", leido.actividades)
        assertNotNull(leido.checkIn)
        assertNotNull(leido.checkOut)
        assertEquals("Conformidad de la jefatura", leido.historial.last().descripcion)
        archivo.delete()
    }
}
