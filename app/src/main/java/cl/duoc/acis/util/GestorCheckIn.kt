package cl.duoc.acis.util

import cl.duoc.acis.repository.OrdenTrabajoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Hace el check-in en segundo plano.
 *
 * Antes la corrutina vivía en el ViewModel del detalle (viewModelScope): si el técnico salía de
 * la pantalla mientras se buscaba el GPS, se cancelaba y el check-in se perdía. Ahora corre en un
 * ámbito propio de la app (scope), así que sigue aunque el técnico vuelva a la lista o abra otra OT.
 * Cuando termina, la OT pasa sola a "En ejecución" (Room avisa a todas las pantallas por su Flow).
 *
 * Es una sola instancia para toda la app (igual que AppDatabase).
 */
class GestorCheckIn(
    private val repository: OrdenTrabajoRepository,
    private val ubicacion: FuenteUbicacion,
    // SupervisorJob: si un check-in falla, no cancela los demás
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) {

    // OT que están buscando ubicación en este momento (para mostrar "Obteniendo ubicación…")
    private val _enCurso = MutableStateFlow<Set<Long>>(emptySet())
    val enCurso: StateFlow<Set<Long>> = _enCurso.asStateFlow()

    // Último resultado de cada OT ("Check-in registrado" o el error), hasta que una pantalla lo muestre
    private val _resultados = MutableStateFlow<Map<Long, String>>(emptyMap())
    val resultados: StateFlow<Map<Long, String>> = _resultados.asStateFlow()

    /**
     * Inicia el check-in y vuelve de inmediato: no bloquea la pantalla.
     * Devuelve un error inmediato (por ejemplo, GPS apagado) o null si quedó en marcha.
     */
    fun iniciar(otId: Long, usuarioId: Long): String? {
        if (otId in _enCurso.value) return null // ya se está buscando la ubicación de esta OT
        if (!ubicacion.estaActivada()) return "Activa la ubicación del teléfono para hacer check-in"
        _enCurso.update { it + otId }
        scope.launch {
            try {
                val punto = ubicacion.obtener()
                val resultado = if (punto == null) {
                    "No se pudo obtener la ubicación. Intenta de nuevo en un lugar abierto"
                } else {
                    // La máquina de estados sigue validando: si la OT cambió mientras tanto, no se guarda
                    repository.checkIn(otId, usuarioId, punto.latitude, punto.longitude) ?: "Check-in registrado"
                }
                _resultados.update { it + (otId to resultado) }
            } finally {
                _enCurso.update { it - otId }
            }
        }
        return null
    }

    fun resultadoMostrado(otId: Long) {
        _resultados.update { it - otId }
    }

    companion object {
        @Volatile
        private var INSTANCE: GestorCheckIn? = null

        fun obtener(repository: OrdenTrabajoRepository, ubicacion: FuenteUbicacion): GestorCheckIn =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: GestorCheckIn(repository, ubicacion).also { INSTANCE = it }
            }
    }
}
