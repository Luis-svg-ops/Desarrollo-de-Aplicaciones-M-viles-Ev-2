package cl.duoc.acis.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.acis.data.remote.PerfilRequest
import cl.duoc.acis.data.remote.Resultado
import cl.duoc.acis.repository.UsuarioRepository
import cl.duoc.acis.util.validarConfirmacion
import cl.duoc.acis.util.validarNombre
import cl.duoc.acis.util.validarPassword
import cl.duoc.acis.util.validarTelefono
import kotlinx.coroutines.launch

/**
 * Editar el perfil propio: nombre y teléfono. Cambiar la contraseña es opcional,
 * pero si se hace hay que escribir la actual (por si alguien deja el teléfono abierto).
 */
class EditarPerfilViewModel(private val repository: UsuarioRepository, usuarioId: Long) : ViewModel() {

    var nombre by mutableStateOf(""); private set
    var telefono by mutableStateOf(""); private set
    var claveActual by mutableStateOf(""); private set
    var claveNueva by mutableStateOf(""); private set
    var confirmacion by mutableStateOf(""); private set

    var errorNombre by mutableStateOf<String?>(null); private set
    var errorTelefono by mutableStateOf<String?>(null); private set
    var errorClaveActual by mutableStateOf<String?>(null); private set
    var errorClaveNueva by mutableStateOf<String?>(null); private set
    var errorConfirmacion by mutableStateOf<String?>(null); private set

    var errorServidor by mutableStateOf<String?>(null); private set
    var cargando by mutableStateOf(false); private set
    var guardado by mutableStateOf(false); private set

    init {
        // Se parte con los datos guardados en Room (el teléfono no se guarda localmente)
        viewModelScope.launch { repository.getById(usuarioId)?.let { nombre = it.nombre } }
    }

    private val cambiaClave get() = claveNueva.isNotEmpty() || claveActual.isNotEmpty()

    fun onNombre(v: String) { nombre = v; errorNombre = validarNombre(v); errorServidor = null }
    fun onTelefono(v: String) { telefono = v; errorTelefono = validarTelefono(v); errorServidor = null }
    fun onClaveActual(v: String) { claveActual = v; errorClaveActual = null; errorServidor = null }
    fun onClaveNueva(v: String) {
        claveNueva = v
        errorClaveNueva = if (v.isEmpty()) null else validarPassword(v)
        if (confirmacion.isNotEmpty()) errorConfirmacion = validarConfirmacion(v, confirmacion)
    }
    fun onConfirmacion(v: String) { confirmacion = v; errorConfirmacion = validarConfirmacion(claveNueva, v) }

    fun guardar() {
        errorNombre = validarNombre(nombre)
        errorTelefono = validarTelefono(telefono)
        if (cambiaClave) {
            errorClaveActual = if (claveActual.isEmpty()) "Ingresa tu contraseña actual" else null
            errorClaveNueva = validarPassword(claveNueva)
            errorConfirmacion = validarConfirmacion(claveNueva, confirmacion)
        }
        val errores = listOf(errorNombre, errorTelefono, errorClaveActual, errorClaveNueva, errorConfirmacion)
        if (cargando || errores.any { it != null }) return
        viewModelScope.launch {
            cargando = true
            val r = repository.actualizarPerfil(
                PerfilRequest(
                    nombre.trim(), telefono.trim(),
                    claveActual.ifEmpty { null }, claveNueva.ifEmpty { null }
                )
            )
            cargando = false
            when (r) {
                is Resultado.Exito -> guardado = true
                is Resultado.Error -> errorServidor = r.mensaje
            }
        }
    }
}
