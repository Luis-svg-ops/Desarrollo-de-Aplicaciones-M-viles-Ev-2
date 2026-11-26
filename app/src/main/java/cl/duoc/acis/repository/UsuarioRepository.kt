package cl.duoc.acis.repository

import cl.duoc.acis.data.UsuarioDao
import cl.duoc.acis.data.remote.AuthApi
import cl.duoc.acis.data.remote.LoginRequest
import cl.duoc.acis.data.remote.CrearUsuarioRequest
import cl.duoc.acis.data.remote.PerfilRequest
import cl.duoc.acis.data.remote.RecuperarRequest
import cl.duoc.acis.data.remote.RegistroRequest
import cl.duoc.acis.data.remote.RestablecerRequest
import cl.duoc.acis.data.remote.UsuarioDto
import cl.duoc.acis.data.remote.Resultado
import cl.duoc.acis.data.remote.Sesion
import cl.duoc.acis.data.remote.aEntidad
import cl.duoc.acis.data.remote.llamarApi
import cl.duoc.acis.model.Usuario
import kotlinx.coroutines.flow.Flow

/**
 * Único punto de acceso a los datos de usuarios. El ViewModel no habla directo con el DAO:
 * si mañana los datos vienen de un servidor, solo cambia esta clase.
 */
class UsuarioRepository(
    private val usuarioDao: UsuarioDao,
    /** ms-auth. Si es null (pruebas), el login se revisa solo contra Room, como antes. */
    private val remoto: AuthApi? = null
) {

    /**
     * Login contra ms-auth: si el correo y la clave son correctos, el servidor entrega un token
     * que se guarda en Sesion y se manda en cada pedido. El usuario se guarda en Room (sin clave).
     * Los errores llegan ya traducidos: "Correo o contraseña incorrectos", "Tu cuenta está pendiente…",
     * "Sin conexión con el servidor…".
     */
    suspend fun iniciarSesion(email: String, password: String): Resultado<Usuario> {
        val api = remoto ?: return login(email, password)
            ?.let { Resultado.Exito(it) } ?: Resultado.Error("Correo o contraseña incorrectos")
        return when (val r = llamarApi { api.login(LoginRequest(email.trim().lowercase(), password)) }) {
            is Resultado.Error -> r
            is Resultado.Exito -> {
                Sesion.token = r.datos.token
                val usuario = r.datos.usuario.aEntidad()
                usuarioDao.upsert(usuario)
                Resultado.Exito(usuario)
            }
        }
    }

    // ---------- Cuenta, perfil y administración (todo pasa por ms-auth) ----------

    private fun api(): AuthApi = remoto ?: error("Sin servidor configurado")

    /** Registro de cliente: queda pendiente hasta que el administrador lo apruebe. */
    suspend fun registrar(pedido: RegistroRequest): Resultado<String> =
        llamarApi { api().registro(pedido).mensaje }

    /** Paso 1 de recuperar la contraseña: el servidor envía un código (la respuesta siempre es la misma). */
    suspend fun pedirCodigo(correo: String): Resultado<String> =
        llamarApi { api().recuperar(RecuperarRequest(correo.trim().lowercase())).mensaje }

    /** Paso 2: código de 6 dígitos + contraseña nueva. */
    suspend fun restablecer(correo: String, codigo: String, claveNueva: String): Resultado<String> =
        llamarApi { api().restablecer(RestablecerRequest(correo.trim().lowercase(), codigo, claveNueva)).mensaje }

    /** Edita el perfil propio en el servidor y actualiza la copia de Room. */
    suspend fun actualizarPerfil(pedido: PerfilRequest): Resultado<Usuario> =
        when (val r = llamarApi { api().editarPerfil(pedido) }) {
            is Resultado.Error -> r
            is Resultado.Exito -> {
                val usuario = r.datos.aEntidad()
                usuarioDao.upsert(usuario)
                Resultado.Exito(usuario)
            }
        }

    suspend fun listarUsuarios(soloPendientes: Boolean): Resultado<List<UsuarioDto>> =
        llamarApi { api().usuarios(if (soloPendientes) "PENDIENTE" else null) }

    suspend fun crearUsuario(pedido: CrearUsuarioRequest): Resultado<UsuarioDto> =
        llamarApi { api().crearUsuario(pedido) }

    suspend fun aprobar(id: Long): Resultado<UsuarioDto> = llamarApi { api().aprobar(id) }

    /** Rechaza un registro pendiente (se borra) o desactiva a un usuario que ya trabajó. */
    suspend fun eliminar(id: Long): Resultado<String> = llamarApi { api().eliminarUsuario(id).mensaje }

    /** Lista de técnicos activos desde ms-auth (la usa el jefe al asignar una OT). */
    suspend fun sincronizarTecnicos() {
        val api = remoto ?: return
        val r = llamarApi { api.tecnicos() }
        if (r is Resultado.Exito) usuarioDao.upsertAll(r.datos.map { it.aEntidad() })
    }


    fun getTecnicos(): Flow<List<Usuario>> = usuarioDao.getTecnicos()

    suspend fun getById(id: Long): Usuario? = usuarioDao.getById(id)

    /** Devuelve el usuario si el correo y la contraseña coinciden; si no, null. */
    suspend fun login(email: String, password: String): Usuario? {
        val usuario = usuarioDao.getByEmail(email.trim().lowercase())
        return if (usuario != null && usuario.password == password) usuario else null
    }
}
