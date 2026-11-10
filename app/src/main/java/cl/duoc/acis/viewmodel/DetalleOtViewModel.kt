package cl.duoc.acis.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.acis.model.AccionOT
import cl.duoc.acis.model.EstadoOT
import cl.duoc.acis.model.MaquinaEstadosOT
import cl.duoc.acis.model.OrdenTrabajo
import cl.duoc.acis.model.OtConDetalle
import cl.duoc.acis.model.Rol
import cl.duoc.acis.repository.OrdenTrabajoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Hasta qué paso normal llegó la OT (0 Asignada, 1 Aceptada, 2 En ejecución), para la línea de progreso.
 * Se deduce de los datos: si tiene check-in llegó a "En ejecución"; una visita fallida ocurre
 * después de aceptar; si tiene técnico, al menos estuvo asignada.
 */
fun pasoAlcanzado(ot: OrdenTrabajo): Int = when {
    ot.checkInFechaHora != null -> 2
    ot.estado == EstadoOT.VISITA_FALLIDA -> 1
    ot.tecnicoId != null -> 0
    else -> -1
}

class DetalleOtViewModel(
    private val repository: OrdenTrabajoRepository,
    private val otId: Long,
    private val rol: Rol,
    private val usuarioId: Long
) : ViewModel() {

    // La OT con su local, equipo y técnico. Se actualiza sola si cambia en la base
    val detalle: StateFlow<OtConDetalle?> = repository.getDetalle(otId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Botones del detalle: los entrega la máquina de estados según el estado actual y el rol
    val acciones: StateFlow<List<AccionOT>> = detalle
        .map { d ->
            if (d == null) emptyList()
            else MaquinaEstadosOT.accionesDisponibles(d.ot.estado, rol).filter { !it.enFormulario }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val puedeEditar: StateFlow<Boolean> = detalle
        .map { d -> rol == Rol.JEFE && d != null && MaquinaEstadosOT.sePuedeEditar(d.ot.estado) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // ---- Diálogo de motivo (rechazar, visita fallida, postergar, cancelar) ----
    private val _accionConMotivo = MutableStateFlow<AccionOT?>(null)
    val accionConMotivo: StateFlow<AccionOT?> = _accionConMotivo.asStateFlow()

    var motivo by mutableStateOf<String?>(null)
        private set

    private val _motivoError = MutableStateFlow<String?>(null)
    val motivoError: StateFlow<String?> = _motivoError.asStateFlow()

    // Mensaje para el Snackbar ("OT aceptada", o el error que devolvió la lógica)
    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()

    /** Botón tocado: si la acción pide motivo se abre el diálogo; si no, se aplica directo. */
    fun onAccion(accion: AccionOT) {
        if (accion.pideMotivo) {
            motivo = null
            _motivoError.value = null
            _accionConMotivo.value = accion
        } else {
            aplicar(accion, null)
        }
    }

    fun onMotivoChange(nuevo: String) {
        motivo = nuevo
        _motivoError.value = null
    }

    fun confirmarMotivo() {
        val accion = _accionConMotivo.value ?: return
        if (motivo == null) {
            _motivoError.value = "Selecciona un motivo"
            return
        }
        _accionConMotivo.value = null
        aplicar(accion, motivo)
    }

    fun cerrarDialogo() {
        _accionConMotivo.value = null
    }

    fun mensajeMostrado() {
        _mensaje.value = null
    }

    private fun aplicar(accion: AccionOT, motivo: String?) {
        viewModelScope.launch {
            val error = repository.aplicar(otId, accion, rol, usuarioId, motivo)
            _mensaje.value = error ?: "Estado actualizado: ${accion.hacia.texto}"
        }
    }
}
