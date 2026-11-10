package cl.duoc.acis.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.acis.model.Usuario
import cl.duoc.acis.repository.UsuarioRepository
import cl.duoc.acis.util.validarEmail
import cl.duoc.acis.util.validarPassword
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Lógica de la pantalla de login (mismo estilo que PersonaViewModel del curso):
 * un StateFlow por campo, otro por cada error y uno que dice si el botón está habilitado.
 * La pantalla solo lee estos estados y llama a las funciones; no valida nada por su cuenta.
 */
class LoginViewModel(private val usuarioRepository: UsuarioRepository) : ViewModel() {

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _emailError = MutableStateFlow<String?>(null)
    val emailError: StateFlow<String?> = _emailError.asStateFlow()

    private val _passwordError = MutableStateFlow<String?>(null)
    val passwordError: StateFlow<String?> = _passwordError.asStateFlow()

    // Error que no es de un campo en particular: correo y contraseña no coinciden
    private val _credencialesError = MutableStateFlow<String?>(null)
    val credencialesError: StateFlow<String?> = _credencialesError.asStateFlow()

    private val _isButtonEnabled = MutableStateFlow(false)
    val isButtonEnabled: StateFlow<Boolean> = _isButtonEnabled.asStateFlow()

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val _verPassword = MutableStateFlow(false)
    val verPassword: StateFlow<Boolean> = _verPassword.asStateFlow()

    fun onEmailChange(nuevo: String) {
        _email.value = nuevo
        _emailError.value = validarEmail(nuevo)
        _credencialesError.value = null
        validate()
    }

    fun onPasswordChange(nuevo: String) {
        _password.value = nuevo
        _passwordError.value = validarPassword(nuevo)
        _credencialesError.value = null
        validate()
    }

    fun alternarVerPassword() {
        _verPassword.value = !_verPassword.value
    }

    // El botón se habilita solo si ambos campos tienen datos y ninguno tiene error
    private fun validate() {
        _isButtonEnabled.value = _email.value.isNotBlank() && _emailError.value == null &&
                _password.value.isNotEmpty() && _passwordError.value == null
    }

    fun iniciarSesion(onSuccess: (Usuario) -> Unit) {
        if (!_isButtonEnabled.value || _cargando.value) return
        viewModelScope.launch {
            _cargando.value = true
            val usuario = usuarioRepository.login(_email.value, _password.value)
            _cargando.value = false
            if (usuario != null) {
                onSuccess(usuario)
            } else {
                _credencialesError.value = "Correo o contraseña incorrectos"
            }
        }
    }
}
