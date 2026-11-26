package cl.duoc.acis.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.acis.data.remote.CrearUsuarioRequest
import cl.duoc.acis.data.remote.Resultado
import cl.duoc.acis.data.remote.UsuarioDto
import cl.duoc.acis.model.Rol
import cl.duoc.acis.repository.UsuarioRepository
import cl.duoc.acis.util.validarEmail
import cl.duoc.acis.util.validarNombre
import cl.duoc.acis.util.validarPassword
import cl.duoc.acis.util.validarTelefono
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Panel del Administrador: ver usuarios, aprobar o rechazar clientes pendientes,
 * crear técnicos o jefes y desactivar cuentas. Todo se hace en ms-auth.
 */
class UsuariosViewModel(private val repository: UsuarioRepository) : ViewModel() {

    /** Estado de la lista (sealed class: el when de la pantalla cubre los 3 casos). */
    sealed class EstadoLista {
        data object Cargando : EstadoLista()
        data class Listo(val usuarios: List<UsuarioDto>) : EstadoLista()
        data class Error(val mensaje: String) : EstadoLista()
    }

    private val _estado = MutableStateFlow<EstadoLista>(EstadoLista.Cargando)
    val estado: StateFlow<EstadoLista> = _estado.asStateFlow()

    /** true = solo los pendientes de aprobación (se abre así: es lo que el admin revisa primero). */
    var soloPendientes by mutableStateOf(true); private set

    /** Mensaje para el Snackbar ("Cliente aprobado", "Usuario desactivado..."). */
    var mensaje by mutableStateOf<String?>(null); private set

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _estado.value = EstadoLista.Cargando
            _estado.value = when (val r = repository.listarUsuarios(soloPendientes)) {
                is Resultado.Exito -> EstadoLista.Listo(r.datos)
                is Resultado.Error -> EstadoLista.Error(r.mensaje)
            }
        }
    }

    fun cambiarFiltro(pendientes: Boolean) {
        soloPendientes = pendientes
        cargar()
    }

    fun aprobar(usuario: UsuarioDto) = ejecutar("${usuario.nombre} aprobado: ya puede iniciar sesión") {
        repository.aprobar(usuario.id)
    }

    /** Pendiente → se rechaza (se borra). Activo → se desactiva (se conserva por el historial). */
    fun eliminar(usuario: UsuarioDto) = ejecutar(null) { repository.eliminar(usuario.id) }

    private fun ejecutar(textoExito: String?, accion: suspend () -> Resultado<Any>) {
        viewModelScope.launch {
            mensaje = when (val r = accion()) {
                is Resultado.Exito -> textoExito ?: (r.datos as? String) ?: "Listo"
                is Resultado.Error -> r.mensaje
            }
            cargar()
        }
    }

    fun mensajeMostrado() {
        mensaje = null
    }

    // ---------- Formulario "Nuevo usuario" (diálogo) ----------

    var formularioAbierto by mutableStateOf(false); private set
    var nombre by mutableStateOf(""); private set
    var correo by mutableStateOf(""); private set
    var telefono by mutableStateOf(""); private set
    var clave by mutableStateOf(""); private set
    var rol by mutableStateOf(Rol.TECNICO); private set

    var errorNombre by mutableStateOf<String?>(null); private set
    var errorCorreo by mutableStateOf<String?>(null); private set
    var errorTelefono by mutableStateOf<String?>(null); private set
    var errorClave by mutableStateOf<String?>(null); private set
    var errorFormulario by mutableStateOf<String?>(null); private set

    /** El admin crea trabajadores; los clientes se registran solos con el código de su local. */
    val rolesQueSePuedenCrear = listOf(Rol.TECNICO, Rol.JEFE, Rol.ADMIN)

    fun abrirFormulario() {
        nombre = ""; correo = ""; telefono = ""; clave = ""; rol = Rol.TECNICO
        errorNombre = null; errorCorreo = null; errorTelefono = null; errorClave = null; errorFormulario = null
        formularioAbierto = true
    }

    fun cerrarFormulario() {
        formularioAbierto = false
    }

    fun onNombre(v: String) { nombre = v; errorNombre = validarNombre(v) }
    fun onCorreo(v: String) { correo = v; errorCorreo = validarEmail(v); errorFormulario = null }
    fun onTelefono(v: String) { telefono = v; errorTelefono = validarTelefono(v) }
    fun onClave(v: String) { clave = v; errorClave = validarPassword(v) }
    fun onRol(v: Rol) { rol = v }

    fun crear() {
        errorNombre = validarNombre(nombre)
        errorCorreo = validarEmail(correo)
        errorTelefono = validarTelefono(telefono)
        errorClave = validarPassword(clave)
        if (listOf(errorNombre, errorCorreo, errorTelefono, errorClave).any { it != null }) return
        viewModelScope.launch {
            val r = repository.crearUsuario(
                CrearUsuarioRequest(nombre.trim(), correo.trim().lowercase(), clave, telefono.trim(), rol, null)
            )
            when (r) {
                is Resultado.Exito -> {
                    formularioAbierto = false
                    mensaje = "${r.datos.nombre} creado como ${rol.texto.lowercase()}"
                    // Se muestra la lista completa para ver al usuario nuevo
                    soloPendientes = false
                    cargar()
                }
                is Resultado.Error -> errorFormulario = r.mensaje
            }
        }
    }
}
