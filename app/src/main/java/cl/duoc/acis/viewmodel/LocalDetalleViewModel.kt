package cl.duoc.acis.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.acis.model.Equipo
import cl.duoc.acis.model.Local
import cl.duoc.acis.model.OtConDetalle
import cl.duoc.acis.repository.EquipoRepository
import cl.duoc.acis.repository.LocalRepository
import cl.duoc.acis.repository.OrdenTrabajoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Detalle de un local: sus datos, sus equipos activos y el historial de OT. */
class LocalDetalleViewModel(
    localRepository: LocalRepository,
    equipoRepository: EquipoRepository,
    otRepository: OrdenTrabajoRepository,
    localId: Long
) : ViewModel() {

    val local: StateFlow<Local?> = localRepository.getById(localId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Solo los equipos activos (los dados de baja no aparecen, pero su historial se conserva)
    val equipos: StateFlow<List<Equipo>> = equipoRepository.getByLocal(localId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val historial: StateFlow<List<OtConDetalle>> = otRepository.getHistorialLocal(localId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
