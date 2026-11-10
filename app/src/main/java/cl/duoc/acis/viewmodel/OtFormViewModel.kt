package cl.duoc.acis.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.acis.model.EstadoOT
import cl.duoc.acis.model.Equipo
import cl.duoc.acis.model.Local
import cl.duoc.acis.model.OrdenTrabajo
import cl.duoc.acis.model.Prioridad
import cl.duoc.acis.model.TipoOT
import cl.duoc.acis.model.Usuario
import cl.duoc.acis.repository.EquipoRepository
import cl.duoc.acis.repository.LocalRepository
import cl.duoc.acis.repository.OrdenTrabajoRepository
import cl.duoc.acis.repository.UsuarioRepository
import cl.duoc.acis.util.validarDescripcion
import cl.duoc.acis.util.validarFechaProgramada
import cl.duoc.acis.util.validarSeleccion
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Formulario para crear o editar una OT (solo el Jefe). Si otId es null, se crea una nueva.
 * Igual que en el login: los valores del formulario usan mutableStateOf y los errores StateFlow.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OtFormViewModel(
    private val otRepository: OrdenTrabajoRepository,
    localRepository: LocalRepository,
    private val equipoRepository: EquipoRepository,
    usuarioRepository: UsuarioRepository,
    private val otId: Long?
) : ViewModel() {

    val esEdicion = otId != null

    // ---- Valores del formulario ----
    var localId by mutableStateOf<Long?>(null)
        private set
    var equipoId by mutableStateOf<Long?>(null)
        private set
    var tecnicoId by mutableStateOf<Long?>(null)
        private set
    var tipo by mutableStateOf(TipoOT.CORRECTIVA)
        private set
    var prioridad by mutableStateOf(Prioridad.MEDIA)
        private set
    var fecha by mutableStateOf<LocalDate?>(LocalDate.now())
        private set
    var hora by mutableStateOf(LocalTime.of(9, 0))
        private set
    var descripcion by mutableStateOf("")
        private set

    // ---- Errores y estado del botón (StateFlow, como en el ejemplo del curso) ----
    private val _localError = MutableStateFlow<String?>(null)
    val localError: StateFlow<String?> = _localError.asStateFlow()

    private val _equipoError = MutableStateFlow<String?>(null)
    val equipoError: StateFlow<String?> = _equipoError.asStateFlow()

    private val _fechaError = MutableStateFlow<String?>(null)
    val fechaError: StateFlow<String?> = _fechaError.asStateFlow()

    private val _descripcionError = MutableStateFlow<String?>(null)
    val descripcionError: StateFlow<String?> = _descripcionError.asStateFlow()

    private val _isButtonEnabled = MutableStateFlow(false)
    val isButtonEnabled: StateFlow<Boolean> = _isButtonEnabled.asStateFlow()

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    // Pasa a true cuando se guardó: la pantalla muestra el aviso y vuelve atrás
    private val _guardado = MutableStateFlow(false)
    val guardado: StateFlow<Boolean> = _guardado.asStateFlow()

    // ---- Listas para los desplegables (vienen de Room) ----
    val locales: StateFlow<List<Local>> = localRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tecnicos: StateFlow<List<Usuario>> = usuarioRepository.getTecnicos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Los equipos dependen del local elegido: cada vez que cambia el local, se cargan sus equipos
    val equipos: StateFlow<List<Equipo>> = snapshotFlow { localId }
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else equipoRepository.getByLocal(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // La OT original cuando se está editando
    private var original: OrdenTrabajo? = null

    init {
        if (otId != null) {
            viewModelScope.launch {
                otRepository.getOt(otId).first()?.let { ot ->
                    original = ot
                    localId = ot.localId
                    equipoId = ot.equipoId
                    tecnicoId = ot.tecnicoId
                    tipo = ot.tipo
                    prioridad = ot.prioridad
                    val programada = Instant.ofEpochMilli(ot.fechaProgramada).atZone(ZoneId.systemDefault())
                    fecha = programada.toLocalDate()
                    hora = programada.toLocalTime()
                    descripcion = ot.descripcion
                    // Si la fecha original ya pasó (por ejemplo, una visita fallida), hay que reprogramar
                    _fechaError.value = validarFechaProgramada(fecha)
                    validate()
                }
            }
        }
    }

    fun onLocalChange(id: Long?) {
        localId = id
        equipoId = null // el equipo anterior era de otro local
        _localError.value = validarSeleccion(id, "Selecciona un local")
        validate()
    }

    fun onEquipoChange(id: Long?) {
        equipoId = id
        _equipoError.value = validarSeleccion(id, "Selecciona un equipo")
        validate()
    }

    fun onTecnicoChange(id: Long?) {
        tecnicoId = id
    }

    fun onTipoChange(nuevo: TipoOT) {
        tipo = nuevo
    }

    fun onPrioridadChange(nueva: Prioridad) {
        prioridad = nueva
    }

    fun onFechaChange(nueva: LocalDate) {
        fecha = nueva
        _fechaError.value = validarFechaProgramada(nueva)
        validate()
    }

    fun onHoraChange(nueva: LocalTime) {
        hora = nueva
    }

    fun onDescripcionChange(nuevo: String) {
        descripcion = nuevo
        _descripcionError.value = validarDescripcion(nuevo)
        validate()
    }

    // Se usan las mismas funciones de util, así el botón y los mensajes nunca se contradicen
    private fun validate() {
        _isButtonEnabled.value = validarSeleccion(localId, "") == null &&
                validarSeleccion(equipoId, "") == null &&
                validarFechaProgramada(fecha) == null &&
                validarDescripcion(descripcion) == null
    }

    fun guardar() {
        if (!_isButtonEnabled.value || _cargando.value) return
        val dia = fecha ?: return
        val local = localId ?: return
        val equipo = equipoId ?: return
        val programada = dia.atTime(hora).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        viewModelScope.launch {
            _cargando.value = true
            val base = original
            if (base == null) {
                // OT nueva: si ya tiene técnico queda Asignada; si no, Creada
                otRepository.crear(
                    OrdenTrabajo(
                        numero = "",
                        localId = local,
                        equipoId = equipo,
                        tecnicoId = tecnicoId,
                        tipo = tipo,
                        prioridad = prioridad,
                        estado = if (tecnicoId == null) EstadoOT.CREADA else EstadoOT.ASIGNADA,
                        descripcion = descripcion.trim(),
                        fechaCreacion = System.currentTimeMillis(),
                        fechaProgramada = programada
                    )
                )
            } else {
                // Edición: si se cambia el técnico, o la OT estaba sin asignar o detenida, vuelve a Asignada
                val estadosParaReasignar = setOf(EstadoOT.CREADA, EstadoOT.RECHAZADA, EstadoOT.VISITA_FALLIDA, EstadoOT.POSTERGADA)
                val nuevoEstado = when {
                    tecnicoId == null -> EstadoOT.CREADA
                    tecnicoId != base.tecnicoId || base.estado in estadosParaReasignar -> EstadoOT.ASIGNADA
                    else -> base.estado
                }
                otRepository.actualizar(
                    base.copy(
                        localId = local,
                        equipoId = equipo,
                        tecnicoId = tecnicoId,
                        tipo = tipo,
                        prioridad = prioridad,
                        estado = nuevoEstado,
                        descripcion = descripcion.trim(),
                        fechaProgramada = programada,
                        // El motivo de la visita fallida o postergación ya no aplica al reprogramar
                        motivo = if (nuevoEstado == EstadoOT.ASIGNADA && base.estado != EstadoOT.ASIGNADA) null else base.motivo
                    )
                )
            }
            _cargando.value = false
            _guardado.value = true
        }
    }
}
