package cl.duoc.acis.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.acis.data.remote.RegistroRequest
import cl.duoc.acis.data.remote.Resultado
import cl.duoc.acis.repository.UsuarioRepository
import cl.duoc.acis.util.validarCodigoLocal
import cl.duoc.acis.util.validarConfirmacion
import cl.duoc.acis.util.validarEmail
import cl.duoc.acis.util.validarNombre
import cl.duoc.acis.util.validarPassword
import cl.duoc.acis.util.validarTelefono
import kotlinx.coroutines.launch

/**
 * Registro de clientes. Cada campo tiene su texto y su error; la validación está en util/Validaciones
 * (fuera de la pantalla) y el servidor vuelve a validar todo. La cuenta queda pendiente de aprobación.
 */
class RegistroViewModel(private val repository: UsuarioRepository) : ViewModel() {

    var nombre by mutableStateOf(""); private set
    var correo by mutableStateOf(""); private set
    var telefono by mutableStateOf(""); private set
    var codigoLocal by mutableStateOf(""); private set
    var clave by mutableStateOf(""); private set
    var confirmacion by mutableStateOf(""); private set

    var errorNombre by mutableStateOf<String?>(null); private set
    var errorCorreo by mutableStateOf<String?>(null); private set
    var errorTelefono by mutableStateOf<String?>(null); private set
    var errorCodigo by mutableStateOf<String?>(null); private set
    var errorClave by mutableStateOf<String?>(null); private set
    var errorConfirmacion by mutableStateOf<String?>(null); private set

    /** Error que manda el servidor (correo repetido, código de local inexistente, sin conexión...). */
    var errorServidor by mutableStateOf<String?>(null); private set
    var cargando by mutableStateOf(false); private set
    /** Mensaje final: "Registro recibido…". La pantalla lo muestra y ofrece volver al login. */
    var listo by mutableStateOf<String?>(null); private set

    fun onNombre(v: String) { nombre = v; errorNombre = validarNombre(v); errorServidor = null }
    fun onCorreo(v: String) { correo = v; errorCorreo = validarEmail(v); errorServidor = null }
    fun onTelefono(v: String) { telefono = v; errorTelefono = validarTelefono(v); errorServidor = null }
    fun onCodigo(v: String) { codigoLocal = v.uppercase(); errorCodigo = validarCodigoLocal(v); errorServidor = null }
    fun onClave(v: String) {
        clave = v
        errorClave = validarPassword(v)
        if (confirmacion.isNotEmpty()) errorConfirmacion = validarConfirmacion(v, confirmacion)
    }
    fun onConfirmacion(v: String) { confirmacion = v; errorConfirmacion = validarConfirmacion(clave, v) }

    /** Valida todos los campos (también los que no se tocaron) y dice si se puede enviar. */
    private fun validarTodo(): Boolean {
        errorNombre = validarNombre(nombre)
        errorCorreo = validarEmail(correo)
        errorTelefono = validarTelefono(telefono)
        errorCodigo = validarCodigoLocal(codigoLocal)
        errorClave = validarPassword(clave)
        errorConfirmacion = validarConfirmacion(clave, confirmacion)
        return listOf(errorNombre, errorCorreo, errorTelefono, errorCodigo, errorClave, errorConfirmacion).all { it == null }
    }

    fun registrar() {
        if (cargando || !validarTodo()) return
        viewModelScope.launch {
            cargando = true
            val r = repository.registrar(
                RegistroRequest(nombre.trim(), correo.trim().lowercase(), clave, telefono.trim(), codigoLocal.trim())
            )
            cargando = false
            when (r) {
                is Resultado.Exito -> listo = r.datos
                is Resultado.Error -> errorServidor = r.mensaje
            }
        }
    }
}
