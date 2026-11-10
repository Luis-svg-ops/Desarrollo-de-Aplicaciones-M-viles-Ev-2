package cl.duoc.acis.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.acis.model.EstadoOT
import cl.duoc.acis.model.OtConDetalle
import cl.duoc.acis.model.Rol
import cl.duoc.acis.repository.OrdenTrabajoRepository
import cl.duoc.acis.util.esHoy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Un filtro de la lista: su nombre y qué estados incluye (null = todos). */
data class FiltroOt(val texto: String, val estados: Set<EstadoOT>?)

class ListaOtViewModel(
    repository: OrdenTrabajoRepository,
    usuarioId: Long,
    rol: Rol
) : ViewModel() {

    // El técnico usa pestañas; el jefe, chips con más opciones
    val filtros: List<FiltroOt> = if (rol == Rol.JEFE) listOf(
        FiltroOt("Todas", null),
        FiltroOt("Sin asignar", setOf(EstadoOT.CREADA, EstadoOT.RECHAZADA, EstadoOT.VISITA_FALLIDA, EstadoOT.POSTERGADA)),
        FiltroOt("En curso", setOf(EstadoOT.ASIGNADA, EstadoOT.ACEPTADA, EstadoOT.EN_EJECUCION)),
        FiltroOt("Cerradas", setOf(EstadoOT.CERRADA, EstadoOT.CANCELADA)),
    ) else listOf(
        FiltroOt("Pendientes", setOf(EstadoOT.ASIGNADA, EstadoOT.ACEPTADA)),
        FiltroOt("En ejecución", setOf(EstadoOT.EN_EJECUCION)),
        // Todo lo que ya salió de la agenda del técnico: cerradas y las que volvieron al jefe
        FiltroOt("Cerradas", setOf(EstadoOT.CERRADA, EstadoOT.VISITA_FALLIDA, EstadoOT.RECHAZADA, EstadoOT.POSTERGADA, EstadoOT.CANCELADA)),
    )

    // Todas las OT del usuario. Es un Flow: si la base cambia, la lista se actualiza sola
    private val todas: StateFlow<List<OtConDetalle>> = repository.getParaUsuario(usuarioId, rol)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _filtroSeleccionado = MutableStateFlow(0)
    val filtroSeleccionado: StateFlow<Int> = _filtroSeleccionado.asStateFlow()

    /** Las OT que pasan el filtro elegido. */
    val otsFiltradas: StateFlow<List<OtConDetalle>> = combine(todas, _filtroSeleccionado) { lista, indice ->
        val estados = filtros[indice].estados
        if (estados == null) lista else lista.filter { it.ot.estado in estados }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Cuántas OT hay en cada filtro (el número al lado de cada pestaña o chip). */
    val conteos: StateFlow<List<Int>> = todas.map { lista ->
        filtros.map { filtro ->
            val estados = filtro.estados
            if (estados == null) lista.size else lista.count { it.ot.estado in estados }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), filtros.map { 0 })

    /**
     * Próxima visita del técnico: la que está en ejecución o, si no hay,
     * la siguiente de hoy (asignada o aceptada) según la hora programada.
     */
    val proximaVisita: StateFlow<OtConDetalle?> = todas.map { lista ->
        val deHoy = lista.filter { esHoy(it.ot.fechaProgramada) }
        deHoy.firstOrNull { it.ot.estado == EstadoOT.EN_EJECUCION }
            ?: deHoy.filter { it.ot.estado == EstadoOT.ASIGNADA || it.ot.estado == EstadoOT.ACEPTADA }
                .minByOrNull { it.ot.fechaProgramada }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun seleccionarFiltro(indice: Int) {
        _filtroSeleccionado.value = indice
    }
}
