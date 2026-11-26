package cl.duoc.acis.viewmodel

import cl.duoc.acis.MainDispatcherRule
import cl.duoc.acis.Muestras
import cl.duoc.acis.data.remote.Resultado
import cl.duoc.acis.model.AccionOT
import cl.duoc.acis.model.EstadoOT
import cl.duoc.acis.model.Rol
import cl.duoc.acis.model.TipoEquipo
import cl.duoc.acis.model.UbicacionEquipo
import cl.duoc.acis.model.Prioridad
import cl.duoc.acis.model.TipoOT
import cl.duoc.acis.repository.ClimaRepository
import cl.duoc.acis.repository.EquipoRepository
import cl.duoc.acis.repository.LocalRepository
import cl.duoc.acis.repository.OrdenTrabajoRepository
import cl.duoc.acis.repository.ResultadoBorrado
import cl.duoc.acis.repository.UsuarioRepository
import cl.duoc.acis.util.GestorCheckIn
import cl.duoc.acis.util.crearClima
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/** ViewModels de las OT (lista, detalle, formulario, cierre) y de equipos/locales, con MockK. */
class OtViewModelsTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val otRepo = mockk<OrdenTrabajoRepository>(relaxed = true)
    private val localRepo = mockk<LocalRepository>(relaxed = true)
    private val equipoRepo = mockk<EquipoRepository>(relaxed = true)
    private val usuarioRepo = mockk<UsuarioRepository>(relaxed = true)

    // ---------- Lista ----------

    @Test
    fun lista_filtraCuentaYMarcaLaProximaVisita() = runTest {
        every { otRepo.getParaUsuario(1, Rol.TECNICO, null) } returns flowOf(
            listOf(Muestras.detalle(EstadoOT.ASIGNADA), Muestras.detalle(EstadoOT.CERRADA))
        )
        coEvery { otRepo.sincronizar() } returns Resultado.Exito(Unit)
        val vm = ListaOtViewModel(otRepo, 1, Rol.TECNICO)
        // Los StateFlow "WhileSubscribed" necesitan a alguien escuchando
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.otsFiltradas.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.conteos.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.proximaVisita.collect {} }

        assertEquals(listOf(1, 0, 1), vm.conteos.value)
        assertEquals(1, vm.otsFiltradas.value.size)
        assertEquals(EstadoOT.ASIGNADA, vm.proximaVisita.value?.ot?.estado)
        vm.seleccionarFiltro(2)
        assertEquals(EstadoOT.CERRADA, vm.otsFiltradas.value.single().ot.estado)
        assertNull(vm.aviso.value)
    }

    @Test
    fun lista_sinSenal_avisaYPermiteReintentar() {
        every { otRepo.getParaUsuario(any(), any(), any()) } returns flowOf(emptyList())
        coEvery { otRepo.sincronizar() } returns Resultado.Error("x", sinConexion = true) andThen Resultado.Error("Tu sesión expiró")
        val vm = ListaOtViewModel(otRepo, 2, Rol.JEFE)
        assertEquals("Sin conexión: mostrando los datos guardados en el teléfono", vm.aviso.value)
        vm.refrescar()
        assertEquals("Tu sesión expiró", vm.aviso.value)
        assertFalse(vm.sincronizando.value)
        assertEquals(5, vm.filtros.size)
        assertEquals(2, ListaOtViewModel(otRepo, 6, Rol.CLIENTE, 2).filtros.size)
    }

    // ---------- Detalle ----------

    private fun detalleVm(estado: EstadoOT, rol: Rol, clima: ClimaRepository? = null): DetalleOtViewModel {
        every { otRepo.getDetalle(2) } returns flowOf(Muestras.detalle(estado))
        every { otRepo.getHistorial(2) } returns flowOf(emptyList())
        val checkIns = mockk<GestorCheckIn>(relaxed = true)
        every { checkIns.enCurso } returns MutableStateFlow(emptySet())
        every { checkIns.resultados } returns MutableStateFlow(mapOf(2L to "Check-in registrado"))
        every { checkIns.iniciar(2, 1) } returns "Activa la ubicación del teléfono para hacer check-in"
        return DetalleOtViewModel(otRepo, 2, rol, 1, checkIns, clima)
    }

    @Test
    fun detalle_botonesSegunRolYEstado_yAcciones() = runTest {
        coEvery { otRepo.aplicar(2, AccionOT.ACEPTAR, Rol.TECNICO, 1, null) } returns null
        val vm = detalleVm(EstadoOT.ASIGNADA, Rol.TECNICO)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.acciones.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.puedeEditar.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.buscandoUbicacion.collect {} }

        assertEquals(listOf(AccionOT.ACEPTAR, AccionOT.RECHAZAR), vm.acciones.value)
        assertFalse(vm.puedeEditar.value)
        // El resultado del check-in en segundo plano llega como mensaje
        assertEquals("Check-in registrado", vm.mensaje.value)
        vm.onAccion(AccionOT.ACEPTAR)
        assertEquals("Estado actualizado: Aceptada", vm.mensaje.value)
        vm.mensajeMostrado()
        vm.hacerCheckIn()
        assertEquals("Activa la ubicación del teléfono para hacer check-in", vm.mensaje.value)
    }

    @Test
    fun detalle_motivo_obligatorio_yOtro() = runTest {
        coEvery { otRepo.aplicar(any(), any(), any(), any(), any()) } returns null
        val vm = detalleVm(EstadoOT.ASIGNADA, Rol.TECNICO)
        vm.onAccion(AccionOT.RECHAZAR)
        assertEquals(AccionOT.RECHAZAR, vm.accionConMotivo.value)
        vm.confirmarMotivo()
        assertEquals("Selecciona un motivo", vm.motivoError.value)
        vm.onMotivoChange("Otro")
        vm.onMotivoOtroChange("ab")
        vm.confirmarMotivo()
        assertEquals("Escribe al menos 5 caracteres", vm.motivoError.value)
        vm.onMotivoOtroChange("Estoy con licencia")
        vm.confirmarMotivo()
        coVerify { otRepo.aplicar(2, AccionOT.RECHAZAR, Rol.TECNICO, 1, "Otro: Estoy con licencia") }
        vm.onAccion(AccionOT.RECHAZAR); vm.cerrarDialogo()
        assertNull(vm.accionConMotivo.value)
    }

    @Test
    fun detalle_jefe_exportaInforme_yVeElClima() = runTest {
        val clima = mockk<ClimaRepository>()
        coEvery { clima.climaEn(any(), any()) } returns Resultado.Exito(crearClima(12.0, 61, 10.0, 2.0))
        val vm = detalleVm(EstadoOT.CERRADA, Rol.JEFE, clima)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.detalle.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.historial.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.puedeExportar.collect {} }

        assertTrue(vm.puedeExportar.value)
        assertTrue(vm.informeJson()!!.contains("OT-2026-0102"))
        assertEquals("informe_OT-2026-0102.json", vm.nombreArchivo())
        vm.avisar("Informe guardado")
        assertEquals("Informe guardado", vm.mensaje.value)
        // Lluvia y equipo en la azotea → recomienda postergar
        val listo = vm.clima.value as DetalleOtViewModel.EstadoClima.Listo
        assertTrue(listo.recomendacion!!.contains("postergar"))

        coEvery { clima.climaEn(any(), any()) } returns Resultado.Error("x", sinConexion = true)
        vm.cargarClima()
        assertEquals("No se pudo consultar el clima (sin conexión)", (vm.clima.value as DetalleOtViewModel.EstadoClima.Error).mensaje)
    }

    // ---------- Formulario de OT ----------

    @Test
    fun formulario_nuevaOt_validaYGuarda() {
        every { localRepo.getAll() } returns flowOf(listOf(Muestras.local))
        every { usuarioRepo.getTecnicos() } returns flowOf(listOf(Muestras.pedro))
        coEvery { otRepo.guardarNueva(any()) } returns "Sin conexión" andThen null
        val vm = OtFormViewModel(otRepo, localRepo, equipoRepo, usuarioRepo, null)
        assertFalse(vm.esEdicion)
        assertFalse(vm.isButtonEnabled.value)

        vm.onLocalChange(null); assertEquals("Selecciona un local", vm.localError.value)
        vm.onLocalChange(2); vm.onEquipoChange(3); vm.onTecnicoChange(1)
        vm.onTipoChange(TipoOT.PREVENTIVA); vm.onPrioridadChange(Prioridad.ALTA)
        vm.onDescripcionChange("corta"); assertEquals("Mínimo 10 caracteres (llevas 5)", vm.descripcionError.value)
        vm.onDescripcionChange("Ruido fuerte en la manejadora")
        vm.onFechaChange(LocalDate.now().plusDays(1)); vm.onHoraChange(LocalTime.of(10, 0))
        assertTrue(vm.isButtonEnabled.value)

        vm.guardar()
        assertEquals("Sin conexión", vm.errorGuardar.value)
        vm.errorMostrado()
        vm.guardar()
        assertTrue(vm.guardado.value)
        coVerify { otRepo.guardarNueva(match { it.tecnicoId == 1L && it.estado == EstadoOT.ASIGNADA && it.prioridad == Prioridad.ALTA }) }
        coVerify { usuarioRepo.sincronizarTecnicos() }
    }

    @Test
    fun formulario_edicion_cargaLaOt_yGuardaLosCambios() {
        every { otRepo.getOt(2) } returns flowOf(Muestras.ot(EstadoOT.ASIGNADA).copy(fechaProgramada = System.currentTimeMillis() + 86_400_000))
        coEvery { otRepo.actualizar(any()) } returns "Una OT cerrada no se puede editar" andThen null
        val vm = OtFormViewModel(otRepo, localRepo, equipoRepo, usuarioRepo, 2)
        assertTrue(vm.esEdicion)
        assertEquals("Mantención preventiva del chiller", vm.descripcion)
        vm.guardar()
        assertEquals("Una OT cerrada no se puede editar", vm.errorGuardar.value)
        vm.guardar()
        assertTrue(vm.guardado.value)
    }

    // ---------- Cierre (diagnóstico) ----------

    @Test
    fun cierre_exigeTextosYFoto() {
        every { otRepo.getOt(2) } returns flowOf(Muestras.ot(EstadoOT.EN_EJECUCION))
        coEvery { otRepo.cerrar(any(), any(), any(), any(), any(), any()) } returns "Sin conexión" andThen null
        val vm = DiagnosticoViewModel(otRepo, 2, 1)
        vm.onDiagnosticoChange("corto"); assertEquals("Mínimo 10 caracteres (llevas 5)", vm.diagnosticoError.value)
        vm.onDiagnosticoChange("Rodamiento del ventilador gastado")
        vm.onActividadesChange("Cambio de rodamiento y prueba")
        vm.onMaterialesChange("Rodamiento 6203")
        vm.onFotoCancelada(); assertEquals("Toma una foto del equipo", vm.fotoError.value)
        assertFalse(vm.isButtonEnabled.value)
        vm.onFotoTomada("content://foto/1")
        assertTrue(vm.isButtonEnabled.value)
        vm.hacerCheckOut()
        assertEquals("Sin conexión", vm.errorGuardar.value)
        vm.errorMostrado()
        vm.hacerCheckOut()
        assertTrue(vm.cerrada.value)
    }

    // ---------- Equipos y locales ----------

    @Test
    fun equipo_nuevo_validaYGuarda() {
        coEvery { equipoRepo.guardar(any()) } returns null
        val vm = EquipoFormViewModel(equipoRepo, 2, null)
        vm.onMarcaChange(""); assertEquals("La marca es obligatoria", vm.marcaError.value)
        vm.onMarcaChange("Samsung"); vm.onModeloChange("Split 12.000")
        vm.onTipoChange(TipoEquipo.REFRIGERACION); vm.onUbicacionChange(UbicacionEquipo.EXTERIOR)
        vm.onFechaChange(LocalDate.now().plusDays(3)); assertEquals("La fecha no puede ser futura", vm.fechaError.value)
        vm.onFechaChange(LocalDate.now().minusDays(3))
        vm.guardar()
        assertEquals("Equipo agregado", vm.terminado.value)
    }

    @Test
    fun equipo_edicion_guardaYElimina() {
        coEvery { equipoRepo.getById(3) } returns Muestras.equipo.copy(fechaUltimaMantencion = 0)
        coEvery { equipoRepo.tieneOt(3) } returns true
        coEvery { equipoRepo.guardar(any()) } returns "Sin conexión" andThen null
        val vm = EquipoFormViewModel(equipoRepo, 2, 3)
        assertTrue(vm.tieneOt.value)
        vm.guardar(); assertEquals("Sin conexión", vm.aviso.value)
        vm.avisoMostrado()
        vm.guardar(); assertEquals("Equipo actualizado", vm.terminado.value)

        coEvery { equipoRepo.eliminarODarDeBaja(any()) } returnsMany listOf(
            ResultadoBorrado.TIENE_OT_EN_CURSO, ResultadoBorrado.ERROR, ResultadoBorrado.DADO_DE_BAJA, ResultadoBorrado.ELIMINADO
        )
        vm.eliminar(); assertTrue(vm.aviso.value!!.contains("OT en curso"))
        vm.eliminar(); assertTrue(vm.aviso.value!!.contains("revisa tu conexión"))
        vm.eliminar(); assertTrue(vm.terminado.value!!.contains("dado de baja"))
        vm.eliminar(); assertEquals("Equipo eliminado", vm.terminado.value)
    }

    @Test
    fun locales_cuentanOtAbiertas_yDetalleYPerfil() = runTest {
        every { localRepo.getAll() } returns flowOf(listOf(Muestras.local))
        every { otRepo.getTodas() } returns flowOf(listOf(Muestras.ot(EstadoOT.ACEPTADA), Muestras.ot(EstadoOT.CONFORME)))
        val vm = LocalesViewModel(localRepo, otRepo)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.locales.collect {} }
        assertEquals(LocalResumen(Muestras.local, totalOt = 2, abiertas = 1), vm.locales.value.single())
        coVerify { localRepo.sincronizar() }

        every { localRepo.getById(2) } returns flowOf(Muestras.local)
        every { equipoRepo.getByLocal(2) } returns flowOf(listOf(Muestras.equipo))
        every { otRepo.getHistorialLocal(2) } returns flowOf(emptyList())
        val detalle = LocalDetalleViewModel(localRepo, equipoRepo, otRepo, 2)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { detalle.local.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { detalle.equipos.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { detalle.historial.collect {} }
        assertEquals("Mall Costanera Oeste", detalle.local.value?.nombre)
        assertEquals(1, detalle.equipos.value.size)

        coEvery { usuarioRepo.getById(1) } returns Muestras.pedro
        assertEquals("Pedro Soto", PerfilViewModel(usuarioRepo, 1).usuario.value?.nombre)
    }
}
