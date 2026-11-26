package cl.duoc.acis.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import cl.duoc.acis.model.EstadoOT
import cl.duoc.acis.model.Local
import cl.duoc.acis.model.MaquinaEstadosOT
import cl.duoc.acis.repository.LocalRepository
import cl.duoc.acis.repository.OrdenTrabajoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Un local con sus números para la lista: cuántas OT tiene y cuántas siguen abiertas. */
data class LocalResumen(val local: Local, val totalOt: Int, val abiertas: Int)

class LocalesViewModel(localRepository: LocalRepository, otRepository: OrdenTrabajoRepository) : ViewModel() {

    init {
        // Locales y equipos desde ms-ordenes (el técnico no tiene permiso: ve los de sus OT)
        viewModelScope.launch { localRepository.sincronizar() }
    }

    // Se combinan dos consultas de Room: si cambia una OT, los números se actualizan solos
    val locales: StateFlow<List<LocalResumen>> =
        combine(localRepository.getAll(), otRepository.getTodas()) { locales, ots ->
            locales.map { local ->
                val delLocal = ots.filter { it.localId == local.id }
                LocalResumen(
                    local = local,
                    totalOt = delLocal.size,
                    abiertas = delLocal.count { it.estado !in MaquinaEstadosOT.estadosFinales && it.estado != EstadoOT.CREADA && it.estado != EstadoOT.CERRADA }
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
