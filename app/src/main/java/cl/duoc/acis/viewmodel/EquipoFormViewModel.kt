package cl.duoc.acis.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.acis.model.Equipo
import cl.duoc.acis.model.TipoEquipo
import cl.duoc.acis.model.UbicacionEquipo
import cl.duoc.acis.repository.EquipoRepository
import cl.duoc.acis.util.validarFechaNoFutura
import cl.duoc.acis.util.validarObligatorio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Crear, editar y eliminar (o dar de baja) un equipo. Solo el Jefe.
 * Mismo patrón que los otros formularios: valores con mutableStateOf, errores y botón con StateFlow.
 */
class EquipoFormViewModel(
    private val repository: EquipoRepository,
    private val localId: Long,
    equipoId: Long?
) : ViewModel() {

    val esEdicion = equipoId != null

    var tipo by mutableStateOf(TipoEquipo.CLIMATIZACION)
        private set
    var ubicacion by mutableStateOf(UbicacionEquipo.INTERIOR)
        private set
    var marca by mutableStateOf("")
        private set
    var modelo by mutableStateOf("")
        private set
    var ultimaMantencion by mutableStateOf<LocalDate?>(null)
        private set

    private val _marcaError = MutableStateFlow<String?>(null)
    val marcaError: StateFlow<String?> = _marcaError.asStateFlow()

    private val _modeloError = MutableStateFlow<String?>(null)
    val modeloError: StateFlow<String?> = _modeloError.asStateFlow()

    private val _fechaError = MutableStateFlow<String?>(null)
    val fechaError: StateFlow<String?> = _fechaError.asStateFlow()

    private val _isButtonEnabled = MutableStateFlow(false)
    val isButtonEnabled: StateFlow<Boolean> = _isButtonEnabled.asStateFlow()

    // true si el equipo tiene OT: el botón dice "Dar de baja" en vez de "Eliminar"
    private val _tieneOt = MutableStateFlow(false)
    val tieneOt: StateFlow<Boolean> = _tieneOt.asStateFlow()

    // Mensaje final ("Equipo agregado", "Equipo dado de baja"...): la pantalla lo muestra y vuelve
    private val _terminado = MutableStateFlow<String?>(null)
    val terminado: StateFlow<String?> = _terminado.asStateFlow()

    private var original: Equipo? = null

    init {
        if (equipoId != null) {
            viewModelScope.launch {
                repository.getById(equipoId)?.let { e ->
                    original = e
                    tipo = e.tipo
                    ubicacion = e.ubicacion
                    marca = e.marca
                    modelo = e.modelo
                    ultimaMantencion = e.fechaUltimaMantencion?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    _tieneOt.value = repository.tieneOt(e.id)
                    validate()
                }
            }
        }
    }

    fun onTipoChange(nuevo: TipoEquipo) { tipo = nuevo }

    fun onUbicacionChange(nueva: UbicacionEquipo) { ubicacion = nueva }

    fun onMarcaChange(nuevo: String) {
        marca = nuevo
        _marcaError.value = validarObligatorio(nuevo, "La marca es obligatoria")
        validate()
    }

    fun onModeloChange(nuevo: String) {
        modelo = nuevo
        _modeloError.value = validarObligatorio(nuevo, "El modelo es obligatorio")
        validate()
    }

    fun onFechaChange(nueva: LocalDate?) {
        ultimaMantencion = nueva
        _fechaError.value = validarFechaNoFutura(nueva)
        validate()
    }

    private fun validate() {
        _isButtonEnabled.value = validarObligatorio(marca, "") == null &&
                validarObligatorio(modelo, "") == null &&
                validarFechaNoFutura(ultimaMantencion) == null
    }

    fun guardar() {
        if (!_isButtonEnabled.value) return
        val fecha = ultimaMantencion?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
        viewModelScope.launch {
            val base = original
            if (base == null) {
                repository.crear(
                    Equipo(
                        localId = localId, tipo = tipo, marca = marca.trim(), modelo = modelo.trim(),
                        ubicacion = ubicacion, fechaUltimaMantencion = fecha
                    )
                )
                _terminado.value = "Equipo agregado"
            } else {
                repository.actualizar(
                    base.copy(
                        tipo = tipo, marca = marca.trim(), modelo = modelo.trim(),
                        ubicacion = ubicacion, fechaUltimaMantencion = fecha
                    )
                )
                _terminado.value = "Equipo actualizado"
            }
        }
    }

    /** Regla de negocio (en el repositorio): con OT se da de baja; sin OT se elimina. */
    fun eliminar() {
        val equipo = original ?: return
        viewModelScope.launch {
            val dadoDeBaja = repository.eliminarODarDeBaja(equipo)
            _terminado.value = if (dadoDeBaja) "Equipo dado de baja (su historial se conserva)" else "Equipo eliminado"
        }
    }
}
