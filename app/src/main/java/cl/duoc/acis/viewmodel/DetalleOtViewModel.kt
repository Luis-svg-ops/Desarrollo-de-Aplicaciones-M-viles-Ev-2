package cl.duoc.acis.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.acis.model.EstadoOT
import cl.duoc.acis.model.OrdenTrabajo
import cl.duoc.acis.model.OtConDetalle
import cl.duoc.acis.repository.OrdenTrabajoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    otId: Long
) : ViewModel() {

    // La OT con su local, equipo y técnico. Se actualiza sola si cambia en la base
    val detalle: StateFlow<OtConDetalle?> = repository.getDetalle(otId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** El técnico acepta la OT: pasa de Asignada a Aceptada y se guarda en Room. */
    fun aceptar() {
        val ot = detalle.value?.ot ?: return
        viewModelScope.launch { repository.aceptar(ot) }
    }
}
