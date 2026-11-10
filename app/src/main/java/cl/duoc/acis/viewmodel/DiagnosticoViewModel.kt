package cl.duoc.acis.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.acis.model.OrdenTrabajo
import cl.duoc.acis.repository.OrdenTrabajoRepository
import cl.duoc.acis.util.validarFoto
import cl.duoc.acis.util.validarTextoCierre
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Diagnóstico y check-out (solo el técnico). Mismo patrón que los otros formularios:
 * valores con mutableStateOf, errores y botón con StateFlow, validación con funciones de util.
 */
class DiagnosticoViewModel(
    private val repository: OrdenTrabajoRepository,
    private val otId: Long,
    private val usuarioId: Long
) : ViewModel() {

    val ot: StateFlow<OrdenTrabajo?> = repository.getOt(otId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ---- Valores ----
    var diagnostico by mutableStateOf("")
        private set
    var actividades by mutableStateOf("")
        private set
    var materiales by mutableStateOf("")
        private set
    // Dirección content:// de la foto tomada (null si aún no hay foto)
    var fotoUri by mutableStateOf<String?>(null)
        private set

    // ---- Errores y estado ----
    private val _diagnosticoError = MutableStateFlow<String?>(null)
    val diagnosticoError: StateFlow<String?> = _diagnosticoError.asStateFlow()

    private val _actividadesError = MutableStateFlow<String?>(null)
    val actividadesError: StateFlow<String?> = _actividadesError.asStateFlow()

    private val _fotoError = MutableStateFlow<String?>(null)
    val fotoError: StateFlow<String?> = _fotoError.asStateFlow()

    private val _isButtonEnabled = MutableStateFlow(false)
    val isButtonEnabled: StateFlow<Boolean> = _isButtonEnabled.asStateFlow()

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val _cerrada = MutableStateFlow(false)
    val cerrada: StateFlow<Boolean> = _cerrada.asStateFlow()

    private val _errorGuardar = MutableStateFlow<String?>(null)
    val errorGuardar: StateFlow<String?> = _errorGuardar.asStateFlow()

    fun onDiagnosticoChange(nuevo: String) {
        diagnostico = nuevo
        _diagnosticoError.value = validarTextoCierre(nuevo, "el diagnóstico")
        validate()
    }

    fun onActividadesChange(nuevo: String) {
        actividades = nuevo
        _actividadesError.value = validarTextoCierre(nuevo, "las actividades realizadas")
        validate()
    }

    fun onMaterialesChange(nuevo: String) {
        materiales = nuevo
    }

    /** La cámara guardó la foto: se registra su dirección. */
    fun onFotoTomada(uri: String) {
        fotoUri = uri
        _fotoError.value = null
        validate()
    }

    /** El técnico canceló la cámara sin tomar foto. */
    fun onFotoCancelada() {
        if (fotoUri == null) _fotoError.value = validarFoto(null)
    }

    private fun validate() {
        _isButtonEnabled.value = validarTextoCierre(diagnostico, "") == null &&
                validarTextoCierre(actividades, "") == null &&
                validarFoto(fotoUri) == null
    }

    fun errorMostrado() {
        _errorGuardar.value = null
    }

    /** Check-out: la máquina de estados valida que la OT esté En ejecución y sea de este técnico. */
    fun hacerCheckOut() {
        val foto = fotoUri
        if (!_isButtonEnabled.value || _cargando.value || foto == null) return
        viewModelScope.launch {
            _cargando.value = true
            val error = repository.cerrar(otId, usuarioId, diagnostico.trim(), actividades.trim(), materiales.trim(), foto)
            _cargando.value = false
            if (error == null) _cerrada.value = true else _errorGuardar.value = error
        }
    }
}
