package cl.duoc.acis.viewmodel

import cl.duoc.acis.MainDispatcherRule
import cl.duoc.acis.Muestras
import cl.duoc.acis.data.remote.Resultado
import cl.duoc.acis.model.Rol
import cl.duoc.acis.repository.UsuarioRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * ViewModels de la cuenta (login, registro, recuperar, perfil) y del panel del administrador.
 * Se "mockea" el repositorio: no hay servidor ni base, solo la lógica de cada pantalla.
 */
class CuentaViewModelsTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val repo = mockk<UsuarioRepository>(relaxed = true)

    // ---------- Login ----------

    @Test
    fun login_botonSoloConDatosValidos_yEntraConExito() {
        val vm = LoginViewModel(repo)
        vm.onEmailChange("malo")
        assertEquals("Formato de correo no válido (ej.: nombre@acis.cl)", vm.emailError.value)
        assertFalse(vm.isButtonEnabled.value)

        coEvery { repo.iniciarSesion("tecnico@acis.cl", "123456") } returns Resultado.Exito(Muestras.pedro)
        vm.onEmailChange("tecnico@acis.cl")
        vm.onPasswordChange("123456")
        vm.alternarVerPassword()
        assertTrue(vm.verPassword.value)
        assertTrue(vm.isButtonEnabled.value)
        var entro: String? = null
        vm.iniciarSesion { entro = it.nombre }
        assertEquals("Pedro Soto", entro)
    }

    @Test
    fun login_errorDelServidor_seMuestraBajoLosCampos() {
        coEvery { repo.iniciarSesion(any(), any()) } returns Resultado.Error("Tu cuenta está pendiente de aprobación")
        val vm = LoginViewModel(repo)
        vm.onEmailChange("jdiaz@acis.cl")
        vm.onPasswordChange("123456")
        vm.iniciarSesion { }
        assertEquals("Tu cuenta está pendiente de aprobación", vm.credencialesError.value)
        // Al volver a escribir, el error se limpia
        vm.onPasswordChange("1234567")
        assertNull(vm.credencialesError.value)
    }

    // ---------- Registro ----------

    private fun llenarRegistro(vm: RegistroViewModel) {
        vm.onNombre("Sofia Lagos"); vm.onCorreo("sofia@correo.cl"); vm.onTelefono("+56 9 1234 5678")
        vm.onCodigo("mall-02"); vm.onClave("secreta1"); vm.onConfirmacion("secreta1")
    }

    @Test
    fun registro_sinDatos_marcaTodosLosErrores_yNoLlamaAlServidor() {
        val vm = RegistroViewModel(repo)
        vm.registrar()
        assertEquals("Ingresa tu nombre", vm.errorNombre)
        assertEquals("Ingresa el código de tu local", vm.errorCodigo)
        assertEquals("Repite la contraseña", vm.errorConfirmacion)
        coVerify(exactly = 0) { repo.registrar(any()) }
    }

    @Test
    fun registro_correcto_mandaElCodigoEnMayusculas() {
        coEvery { repo.registrar(any()) } returns Resultado.Exito("Registro recibido")
        val vm = RegistroViewModel(repo)
        llenarRegistro(vm)
        vm.registrar()
        assertEquals("Registro recibido", vm.listo)
        coVerify { repo.registrar(match { it.codigoLocal == "MALL-02" && it.correo == "sofia@correo.cl" }) }
    }

    @Test
    fun registro_claveDistinta_yErrorDelServidor() {
        val vm = RegistroViewModel(repo)
        llenarRegistro(vm)
        vm.onClave("otraClave")
        assertEquals("Las contraseñas no coinciden", vm.errorConfirmacion)
        vm.onConfirmacion("otraClave")
        coEvery { repo.registrar(any()) } returns Resultado.Error("Código de local no válido")
        vm.registrar()
        assertEquals("Código de local no válido", vm.errorServidor)
        vm.onNombre("Sofia L.")
        assertNull(vm.errorServidor)
    }

    // ---------- Recuperar contraseña ----------

    @Test
    fun recuperar_dosPasos() {
        coEvery { repo.pedirCodigo(any()) } returns Resultado.Exito("Si el correo está registrado, te llegó un código")
        coEvery { repo.restablecer(any(), any(), any()) } returns Resultado.Error("Código incorrecto. Te quedan 4 intentos")
        val vm = RecuperarViewModel(repo)

        vm.pedirCodigo()
        assertEquals("Ingresa tu correo", vm.errorCorreo)
        vm.onCorreo("sofia@correo.cl")
        vm.pedirCodigo()
        assertEquals(2, vm.paso)

        vm.onCodigo("12a34567") // solo dígitos y máximo 6
        assertEquals("123456", vm.codigo)
        vm.onClave("nueva123"); vm.onConfirmacion("nueva123")
        vm.restablecer()
        assertEquals("Código incorrecto. Te quedan 4 intentos", vm.errorServidor)

        coEvery { repo.restablecer(any(), any(), any()) } returns Resultado.Exito("Contraseña actualizada")
        vm.restablecer()
        assertEquals("Contraseña actualizada", vm.listo)
        vm.volverAPedir()
        assertEquals(1, vm.paso)
    }

    @Test
    fun recuperar_sinConexion_seQuedaEnElPaso1() {
        coEvery { repo.pedirCodigo(any()) } returns Resultado.Error("Sin conexión con el servidor", sinConexion = true)
        val vm = RecuperarViewModel(repo)
        vm.onCorreo("sofia@correo.cl")
        vm.pedirCodigo()
        assertEquals(1, vm.paso)
        assertEquals("Sin conexión con el servidor", vm.errorServidor)
        vm.onClave("abc"); assertEquals("Debe tener al menos 6 caracteres", vm.errorClave)
        vm.onConfirmacion("abcd"); assertEquals("Las contraseñas no coinciden", vm.errorConfirmacion)
    }

    // ---------- Editar perfil ----------

    @Test
    fun perfil_cambiarClaveExigeLaActual() {
        coEvery { repo.getById(1) } returns Muestras.pedro
        coEvery { repo.actualizarPerfil(any()) } returns Resultado.Exito(Muestras.pedro)
        val vm = EditarPerfilViewModel(repo, 1)
        assertEquals("Pedro Soto", vm.nombre)

        vm.onTelefono("+56 9 1111 2222")
        vm.onClaveNueva("nueva123"); vm.onConfirmacion("nueva123")
        vm.guardar()
        assertEquals("Ingresa tu contraseña actual", vm.errorClaveActual)
        assertFalse(vm.guardado)

        vm.onClaveActual("123456")
        vm.guardar()
        assertTrue(vm.guardado)
        coVerify { repo.actualizarPerfil(match { it.claveActual == "123456" && it.claveNueva == "nueva123" }) }
    }

    @Test
    fun perfil_soloNombreYTelefono_yErrorDelServidor() {
        coEvery { repo.getById(1) } returns Muestras.pedro
        coEvery { repo.actualizarPerfil(any()) } returns Resultado.Error("Sin conexión")
        val vm = EditarPerfilViewModel(repo, 1)
        vm.onNombre("Pe"); assertEquals("Mínimo 3 caracteres", vm.errorNombre)
        vm.onNombre("Pedro Soto R."); vm.onTelefono("+56 9 1111 2222")
        vm.onClaveNueva(""); vm.onConfirmacion("")
        vm.guardar()
        assertEquals("Sin conexión", vm.errorServidor)
        coVerify { repo.actualizarPerfil(match { it.claveNueva == null && it.claveActual == null }) }
    }

    // ---------- Administrador ----------

    @Test
    fun admin_cargaPendientes_aprueba_yCambiaDeFiltro() {
        coEvery { repo.listarUsuarios(true) } returns Resultado.Exito(listOf(Muestras.usuarioDto()))
        coEvery { repo.listarUsuarios(false) } returns Resultado.Exito(emptyList())
        coEvery { repo.aprobar(9) } returns Resultado.Exito(Muestras.usuarioDto(estado = "ACTIVO"))
        coEvery { repo.eliminar(9) } returns Resultado.Exito("Registro rechazado y eliminado")
        val vm = UsuariosViewModel(repo)

        assertEquals(1, (vm.estado.value as UsuariosViewModel.EstadoLista.Listo).usuarios.size)
        vm.aprobar(Muestras.usuarioDto())
        assertEquals("Sofia Lagos aprobado: ya puede iniciar sesión", vm.mensaje)
        vm.eliminar(Muestras.usuarioDto())
        assertEquals("Registro rechazado y eliminado", vm.mensaje)
        vm.mensajeMostrado()
        assertNull(vm.mensaje)
        vm.cambiarFiltro(false)
        assertFalse(vm.soloPendientes)
    }

    @Test
    fun admin_sinConexion_muestraElError() {
        coEvery { repo.listarUsuarios(any()) } returns Resultado.Error("Sin conexión")
        val vm = UsuariosViewModel(repo)
        assertEquals("Sin conexión", (vm.estado.value as UsuariosViewModel.EstadoLista.Error).mensaje)
    }

    @Test
    fun admin_creaUnTecnico() {
        coEvery { repo.listarUsuarios(any()) } returns Resultado.Exito(emptyList())
        coEvery { repo.crearUsuario(any()) } returns Resultado.Error("Ese correo ya está registrado") andThen
                Resultado.Exito(Muestras.usuarioDto(rol = Rol.TECNICO, estado = "ACTIVO"))
        val vm = UsuariosViewModel(repo)
        vm.abrirFormulario()
        assertTrue(vm.formularioAbierto)
        vm.crear()
        assertEquals("Ingresa tu nombre", vm.errorNombre)

        vm.onNombre("Tomás Vidal"); vm.onCorreo("tvidal@acis.cl"); vm.onTelefono("+56 9 2222 3333")
        vm.onClave("123456"); vm.onRol(Rol.JEFE); vm.onRol(Rol.TECNICO)
        vm.crear()
        assertEquals("Ese correo ya está registrado", vm.errorFormulario)
        vm.crear()
        assertFalse(vm.formularioAbierto)
        assertTrue(vm.mensaje!!.contains("creado"))
        vm.cerrarFormulario()
    }
}
