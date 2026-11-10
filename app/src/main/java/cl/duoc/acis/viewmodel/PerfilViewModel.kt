package cl.duoc.acis.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.acis.model.Usuario
import cl.duoc.acis.repository.UsuarioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Datos del usuario que inició sesión, leídos desde Room. */
class PerfilViewModel(repository: UsuarioRepository, usuarioId: Long) : ViewModel() {

    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario.asStateFlow()

    init {
        viewModelScope.launch { _usuario.value = repository.getById(usuarioId) }
    }
}

/** Iniciales para el avatar: "Pedro Soto" → "PS". */
fun iniciales(nombre: String): String =
    nombre.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
