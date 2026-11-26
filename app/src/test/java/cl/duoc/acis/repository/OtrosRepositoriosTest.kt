package cl.duoc.acis.repository

import cl.duoc.acis.Muestras
import cl.duoc.acis.data.EquipoDao
import cl.duoc.acis.data.LocalDao
import cl.duoc.acis.data.OrdenTrabajoDao
import cl.duoc.acis.data.UsuarioDao
import cl.duoc.acis.data.remote.AuthApi
import cl.duoc.acis.data.remote.ClimaActualDto
import cl.duoc.acis.data.remote.ClimaApi
import cl.duoc.acis.data.remote.ClimaResponse
import cl.duoc.acis.data.remote.CrearUsuarioRequest
import cl.duoc.acis.data.remote.LocalDto
import cl.duoc.acis.data.remote.LoginResponse
import cl.duoc.acis.data.remote.MensajeDto
import cl.duoc.acis.data.remote.OrdenesApi
import cl.duoc.acis.data.remote.PerfilRequest
import cl.duoc.acis.data.remote.RegistroRequest
import cl.duoc.acis.data.remote.Resultado
import cl.duoc.acis.data.remote.Sesion
import cl.duoc.acis.model.Rol
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** Repositorios de usuarios, equipos, locales y clima, con MockK. */
class OtrosRepositoriosTest {

    private val usuarioDao = mockk<UsuarioDao>(relaxed = true)
    private val equipoDao = mockk<EquipoDao>(relaxed = true)
    private val otDao = mockk<OrdenTrabajoDao>(relaxed = true)
    private val localDao = mockk<LocalDao>(relaxed = true)
    private val auth = mockk<AuthApi>()
    private val ordenes = mockk<OrdenesApi>()

    @After
    fun limpiarSesion() = Sesion.cerrar()

    // ---------- Usuarios (ms-auth) ----------

    @Test
    fun login_correcto_guardaElToken_yElUsuarioSinClave() = runTest {
        coEvery { auth.login(any()) } returns LoginResponse("token-123", "Bearer", 28800, Muestras.usuarioDto(estado = "ACTIVO"))
        val r = UsuarioRepository(usuarioDao, auth).iniciarSesion(" Sofia@Correo.cl ", "secreta1")
        assertEquals("Sofia Lagos", (r as Resultado.Exito).datos.nombre)
        assertEquals("token-123", Sesion.token)
        coVerify { auth.login(match { it.correo == "sofia@correo.cl" }) }
        coVerify { usuarioDao.upsert(match { it.password.isEmpty() }) }
    }

    @Test
    fun login_pendiente_muestraElMensajeDelServidor() = runTest {
        coEvery { auth.login(any()) } throws Muestras.errorHttp(403, "Tu cuenta está pendiente de aprobación")
        val r = UsuarioRepository(usuarioDao, auth).iniciarSesion("a@b.cl", "secreta1")
        assertEquals("Tu cuenta está pendiente de aprobación", (r as Resultado.Error).mensaje)
        assertNull(Sesion.token)
    }

    @Test
    fun login_sinServidor_revisaContraRoom() = runTest {
        coEvery { usuarioDao.getByEmail("tecnico@acis.cl") } returns Muestras.pedro.copy(password = "123456")
        val repo = UsuarioRepository(usuarioDao)
        assertTrue(repo.iniciarSesion("tecnico@acis.cl", "123456") is Resultado.Exito)
        assertTrue(repo.iniciarSesion("tecnico@acis.cl", "otra") is Resultado.Error)
    }

    @Test
    fun cuenta_registro_recuperar_perfil() = runTest {
        val repo = UsuarioRepository(usuarioDao, auth)
        coEvery { auth.registro(any()) } returns MensajeDto("Registro recibido")
        coEvery { auth.recuperar(any()) } returns MensajeDto("Si el correo está registrado, te llegó un código")
        coEvery { auth.restablecer(any()) } returns MensajeDto("Contraseña actualizada")
        coEvery { auth.editarPerfil(any()) } returns Muestras.usuarioDto(estado = "ACTIVO")

        assertEquals("Registro recibido", (repo.registrar(RegistroRequest("Sofia Lagos", "s@c.cl", "secreta1", "+56 9 1234 5678", "MALL-02")) as Resultado.Exito).datos)
        assertTrue(repo.pedirCodigo(" S@C.cl ") is Resultado.Exito)
        coVerify { auth.recuperar(match { it.correo == "s@c.cl" }) }
        assertTrue(repo.restablecer("s@c.cl", "123456", "nueva123") is Resultado.Exito)
        assertTrue(repo.actualizarPerfil(PerfilRequest("Sofia", "+56 9 1234 5678", null, null)) is Resultado.Exito)
        coVerify { usuarioDao.upsert(any()) }
    }

    @Test
    fun perfil_claveActualMala_noActualizaRoom() = runTest {
        coEvery { auth.editarPerfil(any()) } throws Muestras.errorHttp(400, "La contraseña actual no es correcta")
        val r = UsuarioRepository(usuarioDao, auth).actualizarPerfil(PerfilRequest("Sofia", "+56 9 1234 5678", "mala", "nueva123"))
        assertEquals("La contraseña actual no es correcta", (r as Resultado.Error).mensaje)
        coVerify(exactly = 0) { usuarioDao.upsert(any()) }
    }

    @Test
    fun admin_listar_crear_aprobar_eliminar_yTecnicos() = runTest {
        val repo = UsuarioRepository(usuarioDao, auth)
        coEvery { auth.usuarios("PENDIENTE") } returns listOf(Muestras.usuarioDto())
        coEvery { auth.crearUsuario(any()) } returns Muestras.usuarioDto(rol = Rol.TECNICO, estado = "ACTIVO")
        coEvery { auth.aprobar(9) } returns Muestras.usuarioDto(estado = "ACTIVO")
        coEvery { auth.eliminarUsuario(9) } returns MensajeDto("Registro rechazado y eliminado")
        coEvery { auth.tecnicos() } returns listOf(Muestras.usuarioDto(rol = Rol.TECNICO, estado = "ACTIVO"))

        assertEquals(1, (repo.listarUsuarios(soloPendientes = true) as Resultado.Exito).datos.size)
        assertTrue(repo.crearUsuario(CrearUsuarioRequest("Tomás Vidal", "t@acis.cl", "123456", "+56 9 2222 3333", Rol.TECNICO, null)) is Resultado.Exito)
        assertEquals("ACTIVO", (repo.aprobar(9) as Resultado.Exito).datos.estado)
        assertEquals("Registro rechazado y eliminado", (repo.eliminar(9) as Resultado.Exito).datos)
        repo.sincronizarTecnicos()
        coVerify { usuarioDao.upsertAll(match { it.single().rol == Rol.TECNICO }) }
    }

    // ---------- Equipos ----------

    @Test
    fun equipo_guardar_conServidor_yErrores() = runTest {
        val repo = EquipoRepository(equipoDao, otDao, ordenes)
        coEvery { ordenes.crearEquipo(any()) } returns Muestras.otDto().equipo
        coEvery { ordenes.editarEquipo(3, any()) } throws IOException()
        assertNull(repo.guardar(Muestras.equipo.copy(id = 0)))
        coVerify { equipoDao.upsertAll(any()) }
        assertTrue(repo.guardar(Muestras.equipo)!!.startsWith("Sin conexión"))
    }

    @Test
    fun equipo_eliminar_conServidor_traduceLaRespuesta() = runTest {
        val repo = EquipoRepository(equipoDao, otDao, ordenes)
        coEvery { ordenes.eliminarEquipo(3) } returns MensajeDto("El equipo tiene historial: se dio de baja")
        assertEquals(ResultadoBorrado.DADO_DE_BAJA, repo.eliminarODarDeBaja(Muestras.equipo))
        coEvery { ordenes.eliminarEquipo(3) } returns MensajeDto("Equipo eliminado")
        assertEquals(ResultadoBorrado.ELIMINADO, repo.eliminarODarDeBaja(Muestras.equipo))
        coEvery { ordenes.eliminarEquipo(3) } throws Muestras.errorHttp(409, "El equipo tiene una OT en curso: termínala")
        assertEquals(ResultadoBorrado.TIENE_OT_EN_CURSO, repo.eliminarODarDeBaja(Muestras.equipo))
        coEvery { ordenes.eliminarEquipo(3) } throws IOException()
        assertEquals(ResultadoBorrado.ERROR, repo.eliminarODarDeBaja(Muestras.equipo))
    }

    @Test
    fun equipo_soloRoom_mismasReglas() = runTest {
        val repo = EquipoRepository(equipoDao, otDao)
        coEvery { otDao.contarAbiertasPorEquipo(3) } returns 1
        assertEquals(ResultadoBorrado.TIENE_OT_EN_CURSO, repo.eliminarODarDeBaja(Muestras.equipo))
        coEvery { otDao.contarAbiertasPorEquipo(3) } returns 0
        coEvery { otDao.contarPorEquipo(3) } returns 2
        assertEquals(ResultadoBorrado.DADO_DE_BAJA, repo.eliminarODarDeBaja(Muestras.equipo))
        assertTrue(repo.tieneOt(3))
        coEvery { otDao.contarPorEquipo(3) } returns 0
        assertEquals(ResultadoBorrado.ELIMINADO, repo.eliminarODarDeBaja(Muestras.equipo))
        assertNull(repo.guardar(Muestras.equipo.copy(id = 0)))
        assertNull(repo.guardar(Muestras.equipo))
        coVerify { equipoDao.insert(any()) }
        coVerify { equipoDao.update(Muestras.equipo) }
    }

    // ---------- Locales y clima ----------

    @Test
    fun locales_sincronizar_traeLocalesYEquipos() = runTest {
        val repo = LocalRepository(localDao, equipoDao, ordenes)
        coEvery { ordenes.locales() } returns listOf(LocalDto(2, "MALL-02", "Mall", "Av. 1", "Cerrillos", "Ana", "+56 9 1111 1111", null, null))
        coEvery { ordenes.equipos(2) } returns listOf(Muestras.otDto().equipo)
        repo.sincronizar()
        coVerify { localDao.upsertAll(match { it.single().codigo == "MALL-02" }) }
        coVerify { equipoDao.upsertAll(any()) }
        // Sin servidor no hace nada
        LocalRepository(localDao).sincronizar()
    }

    @Test
    fun clima_traduceLaRespuestaDeOpenMeteo() = runTest {
        val api = mockk<ClimaApi>()
        coEvery { api.actual(any(), any(), any(), any()) } returns ClimaResponse(ClimaActualDto("2026-11-26T15:00", 24.6, 0.0, 63, 12.4))
        val r = ClimaRepository(api).climaEn(-33.4, -70.6)
        assertEquals("Lluvia", (r as Resultado.Exito).datos.descripcion)
        assertEquals(25, r.datos.temperatura)
        coEvery { api.actual(any(), any(), any(), any()) } throws IOException()
        assertTrue((ClimaRepository(api).climaEn(0.0, 0.0) as Resultado.Error).sinConexion)
    }
}
