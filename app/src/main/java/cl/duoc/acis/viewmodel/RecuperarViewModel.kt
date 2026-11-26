package cl.duoc.acis.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.acis.data.remote.Resultado
import cl.duoc.acis.repository.UsuarioRepository
import cl.duoc.acis.util.validarCodigoRecuperacion
import cl.duoc.acis.util.validarConfirmacion
import cl.duoc.acis.util.validarEmail
import cl.duoc.acis.util.validarPassword
import kotlinx.coroutines.launch

/**
 * Recuperar contraseña en 2 pasos:
 * 1) el correo → el servidor manda un código de 6 dígitos (vence en 10 min, 5 intentos);
 * 2) el código + la contraseña nueva.
 */
class RecuperarViewModel(private val repository: UsuarioRepository) : ViewModel() {

    /** 1 = pedir el código, 2 = escribir el código y la clave nueva. */
    var paso by mutableStateOf(1); private set

    var correo by mutableStateOf(""); private set
    var codigo by mutableStateOf(""); private set
    var clave by mutableStateOf(""); private set
    var confirmacion by mutableStateOf(""); private set

    var errorCorreo by mutableStateOf<String?>(null); private set
    var errorCodigo by mutableStateOf<String?>(null); private set
    var errorClave by mutableStateOf<String?>(null); private set
    var errorConfirmacion by mutableStateOf<String?>(null); private set

    var errorServidor by mutableStateOf<String?>(null); private set
    /** Texto informativo del servidor ("Si el correo está registrado, te llegó un código…"). */
    var info by mutableStateOf<String?>(null); private set
    var cargando by mutableStateOf(false); private set
    var listo by mutableStateOf<String?>(null); private set

    fun onCorreo(v: String) { correo = v; errorCorreo = validarEmail(v); errorServidor = null }
    fun onCodigo(v: String) { codigo = v.filter { it.isDigit() }.take(6); errorCodigo = validarCodigoRecuperacion(codigo); errorServidor = null }
    fun onClave(v: String) {
        clave = v
        errorClave = validarPassword(v)
        if (confirmacion.isNotEmpty()) errorConfirmacion = validarConfirmacion(v, confirmacion)
    }
    fun onConfirmacion(v: String) { confirmacion = v; errorConfirmacion = validarConfirmacion(clave, v) }

    fun pedirCodigo() {
        errorCorreo = validarEmail(correo)
        if (cargando || errorCorreo != null) return
        viewModelScope.launch {
            cargando = true
            val r = repository.pedirCodigo(correo)
            cargando = false
            when (r) {
                is Resultado.Exito -> { info = r.datos; paso = 2 }
                is Resultado.Error -> errorServidor = r.mensaje
            }
        }
    }

    fun restablecer() {
        errorCodigo = validarCodigoRecuperacion(codigo)
        errorClave = validarPassword(clave)
        errorConfirmacion = validarConfirmacion(clave, confirmacion)
        if (cargando || listOf(errorCodigo, errorClave, errorConfirmacion).any { it != null }) return
        viewModelScope.launch {
            cargando = true
            val r = repository.restablecer(correo, codigo, clave)
            cargando = false
            when (r) {
                is Resultado.Exito -> listo = r.datos
                // Código incorrecto: el servidor dice cuántos intentos quedan
                is Resultado.Error -> errorServidor = r.mensaje
            }
        }
    }

    /** Volver al paso 1 (por ejemplo, para pedir un código nuevo). */
    fun volverAPedir() {
        paso = 1
        codigo = ""
        errorServidor = null
    }
}
