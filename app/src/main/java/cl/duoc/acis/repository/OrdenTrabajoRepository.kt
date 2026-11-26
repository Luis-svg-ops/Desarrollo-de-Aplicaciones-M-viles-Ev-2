package cl.duoc.acis.repository

import cl.duoc.acis.data.EquipoDao
import cl.duoc.acis.data.EventoDao
import cl.duoc.acis.data.UsuarioDao
import cl.duoc.acis.data.LocalDao
import cl.duoc.acis.data.remote.AccionRequest
import cl.duoc.acis.data.remote.CheckInRequest
import cl.duoc.acis.data.remote.CierreRequest
import cl.duoc.acis.data.remote.OrdenesApi
import cl.duoc.acis.data.remote.OtDto
import cl.duoc.acis.data.remote.Resultado
import cl.duoc.acis.data.remote.aEntidad
import cl.duoc.acis.data.remote.aPedido
import cl.duoc.acis.data.remote.llamarApi
import cl.duoc.acis.data.remote.tecnicoDeOt
import cl.duoc.acis.model.EstadoOT
import cl.duoc.acis.model.EventoOT
import cl.duoc.acis.data.OrdenTrabajoDao
import cl.duoc.acis.model.AccionOT
import cl.duoc.acis.model.MaquinaEstadosOT
import cl.duoc.acis.model.OrdenTrabajo
import cl.duoc.acis.model.OtConDetalle
import cl.duoc.acis.model.Rol
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class OrdenTrabajoRepository(
    private val ordenTrabajoDao: OrdenTrabajoDao,
    // Para actualizar la última mantención del equipo al cerrar una OT
    private val equipoDao: EquipoDao,
    // Historial de cambios de cada OT y nombres de quién los hizo
    private val eventoDao: EventoDao,
    private val usuarioDao: UsuarioDao,
    private val localDao: LocalDao? = null,
    /**
     * ms-ordenes. Si está, el servidor es la fuente de la verdad y Room guarda una copia.
     * Si es null (pruebas sin servidor), todo funciona solo con Room, como antes.
     */
    private val remoto: OrdenesApi? = null
) {

    // ---------- Copia local de lo que entrega el servidor ----------

    /** Guarda en Room una OT que llegó del servidor, con su local, equipo y técnico (en ese orden por las llaves foráneas). */
    private suspend fun guardarCopia(dto: OtDto) {
        localDao?.upsertAll(listOf(dto.local.aEntidad()))
        equipoDao.upsertAll(listOf(dto.equipo.aEntidad()))
        dto.tecnicoId?.let { id -> if (usuarioDao.getById(id) == null) usuarioDao.upsert(tecnicoDeOt(id, dto.tecnicoNombre)) }
        ordenTrabajoDao.upsertAll(listOf(dto.aEntidad()))
    }

    /**
     * Trae del servidor las OT que le corresponden al usuario (el servidor filtra con el token)
     * y deja Room igual. Devuelve el error, o null si se sincronizó. Si no hay señal, Room
     * conserva la última copia: el técnico igual ve sus OT.
     */
    suspend fun sincronizar(): Resultado<Unit> {
        val api = remoto ?: return Resultado.Exito(Unit)
        return when (val r = llamarApi { api.ordenes() }) {
            is Resultado.Error -> r
            is Resultado.Exito -> {
                r.datos.forEach { guardarCopia(it) }
                val ids = r.datos.map { it.id }
                eventoDao.borrarExcepto(ids)
                ordenTrabajoDao.borrarExcepto(ids)
                Resultado.Exito(Unit)
            }
        }
    }

    /** Trae el historial de una OT desde el servidor (si no hay señal, se muestra el guardado). */
    suspend fun sincronizarHistorial(otId: Long) {
        val api = remoto ?: return
        val r = llamarApi { api.historial(otId) }
        if (r is Resultado.Exito) {
            eventoDao.borrarDeOt(otId)
            eventoDao.insertAll(r.datos.map { it.aEntidad(otId) })
        }
    }

    /** Llama al servidor; si responde bien guarda la OT en Room y devuelve null, si no, el mensaje de error. */
    private suspend fun enServidor(llamada: suspend (OrdenesApi) -> OtDto): String? {
        val api = remoto ?: return "Sin servidor configurado"
        return when (val r = llamarApi { llamada(api) }) {
            is Resultado.Error -> r.mensaje
            is Resultado.Exito -> {
                guardarCopia(r.datos)
                sincronizarHistorial(r.datos.id)
                null
            }
        }
    }

    /** OT nueva desde el formulario: al servidor si hay; si no, solo en Room. Devuelve el error o null. */
    suspend fun guardarNueva(ot: OrdenTrabajo): String? =
        if (remoto != null) enServidor { it.crear(ot.aPedido()) } else { crear(ot); null }

    /** Línea de tiempo de la OT: qué pasó, cuándo y quién. */
    fun getHistorial(otId: Long): Flow<List<EventoOT>> = eventoDao.getByOt(otId)

    private suspend fun registrar(otId: Long, estado: EstadoOT, descripcion: String) =
        eventoDao.insert(EventoOT(otId = otId, estado = estado, descripcion = descripcion, fecha = System.currentTimeMillis()))

    private suspend fun nombre(usuarioId: Long?): String =
        usuarioId?.let { usuarioDao.getById(it)?.nombre } ?: "sin técnico"

    /** Texto del historial para cada acción ("Rechazada por Pedro Soto: Fuera de mi zona"). */
    private fun describir(accion: AccionOT, quien: String, motivo: String?): String {
        val conMotivo = motivo?.let { ": $it" } ?: ""
        return when (accion) {
            AccionOT.ACEPTAR -> "Aceptada por $quien"
            AccionOT.RECHAZAR -> "Rechazada por $quien$conMotivo"
            AccionOT.CHECK_IN -> "Check-in de $quien en el local"
            AccionOT.VISITA_FALLIDA -> "Visita fallida$conMotivo"
            AccionOT.CERRAR -> "Trabajo terminado por $quien (diagnóstico y foto)"
            AccionOT.CONFORMIDAD -> "Conformidad de la jefatura"
            AccionOT.DISCONFORMIDAD -> "Disconformidad de la jefatura$conMotivo"
            AccionOT.POSTERGAR -> "Postergada$conMotivo"
            AccionOT.CANCELAR -> "Cancelada$conMotivo"
            AccionOT.ASIGNAR, AccionOT.QUITAR_TECNICO -> accion.texto
        }
    }

    /** El Jefe ve todas las OT; el Técnico, solo las que tiene asignadas. */
    fun getParaUsuario(usuarioId: Long, rol: Rol, localId: Long? = null): Flow<List<OtConDetalle>> = when (rol) {
        Rol.JEFE, Rol.ADMIN -> ordenTrabajoDao.getAllConDetalle()
        Rol.TECNICO -> ordenTrabajoDao.getByTecnicoConDetalle(usuarioId)
        // El cliente solo ve las OT de su local
        Rol.CLIENTE -> ordenTrabajoDao.getByLocalConDetalle(localId ?: -1)
    }

    fun getByLocal(localId: Long): Flow<List<OrdenTrabajo>> = ordenTrabajoDao.getByLocal(localId)

    fun getTodas(): Flow<List<OrdenTrabajo>> = ordenTrabajoDao.getAll()

    /** Historial de OT de un local, con su equipo y técnico. */
    fun getHistorialLocal(localId: Long): Flow<List<OtConDetalle>> = ordenTrabajoDao.getByLocalConDetalle(localId)

    fun getDetalle(id: Long): Flow<OtConDetalle?> = ordenTrabajoDao.getConDetalle(id)

    fun getOt(id: Long): Flow<OrdenTrabajo?> = ordenTrabajoDao.getById(id)

    /**
     * Crea la OT y le asigna su número correlativo (OT-2026-0108) a partir del id que entrega Room.
     * Devuelve el id de la OT nueva.
     */
    suspend fun crear(ot: OrdenTrabajo): Long {
        val id = ordenTrabajoDao.insert(ot)
        val numero = "OT-%d-%04d".format(LocalDate.now().year, 100 + id)
        ordenTrabajoDao.update(ot.copy(id = id, numero = numero))
        registrar(id, EstadoOT.CREADA, "Creada por la jefatura")
        if (ot.tecnicoId != null) registrar(id, EstadoOT.ASIGNADA, "Asignada a ${nombre(ot.tecnicoId)}")
        return id
    }

    /**
     * Guarda los cambios del formulario de edición. El nuevo estado lo calcula la máquina de estados;
     * si la OT ya no se puede editar (por ejemplo, se cerró mientras el Jefe la editaba), no se guarda.
     * Devuelve el mensaje de error o null si se guardó.
     */
    suspend fun actualizar(ot: OrdenTrabajo): String? {
        if (remoto != null) return enServidor { it.editar(ot.id, ot.aPedido()) }
        val actual = ordenTrabajoDao.getById(ot.id).first() ?: return "La OT ya no existe"
        if (!MaquinaEstadosOT.sePuedeEditar(actual.estado)) {
            return "Una OT ${actual.estado.texto.lowercase()} no se puede editar"
        }
        val nuevoEstado = MaquinaEstadosOT.estadoAlEditar(actual.estado, actual.tecnicoId, ot.tecnicoId)
        ordenTrabajoDao.update(
            ot.copy(
                estado = nuevoEstado,
                // Al reasignar o reprogramar, el motivo anterior (rechazo, visita fallida...) ya no aplica
                motivo = if (nuevoEstado != actual.estado) null else actual.motivo,
                // Reasignada o sin técnico: el (nuevo) técnico tiene que volver a aceptarla
                fechaAceptacion = if (nuevoEstado != actual.estado) null else actual.fechaAceptacion
            )
        )
        registrar(
            ot.id, nuevoEstado,
            when {
                nuevoEstado == EstadoOT.CREADA && actual.estado != EstadoOT.CREADA -> "Técnico quitado por la jefatura"
                nuevoEstado == EstadoOT.ASIGNADA && ot.tecnicoId != actual.tecnicoId -> "Asignada a ${nombre(ot.tecnicoId)}"
                nuevoEstado != actual.estado -> "Reprogramada para ${nombre(ot.tecnicoId)}"
                else -> "Datos editados por la jefatura"
            }
        )
        return null
    }

    /**
     * Único camino para cambiar el estado de una OT (aceptar, rechazar, postergar, cancelar...).
     * Lee el estado actual desde Room y le pregunta a la máquina de estados si el cambio está
     * permitido. Devuelve el mensaje de error o null si se guardó.
     */
    suspend fun aplicar(
        otId: Long,
        accion: AccionOT,
        rol: Rol,
        usuarioId: Long,
        motivo: String? = null,
        // Datos extra que se guardan junto con el cambio (por ejemplo, las coordenadas del check-in)
        datos: (OrdenTrabajo) -> OrdenTrabajo = { it }
    ): String? {
        // Con servidor: él valida con su propia máquina de estados ("nunca confíes en el cliente")
        if (remoto != null) return enServidor { it.accion(otId, AccionRequest(accion.name, motivo)) }
        val actual = ordenTrabajoDao.getById(otId).first() ?: return "La OT ya no existe"
        MaquinaEstadosOT.validar(accion, actual.estado, rol, motivo, actual.tecnicoId, usuarioId)?.let { return it }
        val cambiada = actual.copy(
            estado = accion.hacia,
            motivo = if (accion.pideMotivo) motivo else actual.motivo,
            // Se guarda cuándo aceptó: así la línea de progreso sabe hasta dónde llegó si después se detiene
            fechaAceptacion = if (accion == AccionOT.ACEPTAR) System.currentTimeMillis() else actual.fechaAceptacion
        )
        ordenTrabajoDao.update(datos(cambiada))
        registrar(otId, accion.hacia, describir(accion, nombre(usuarioId), motivo))
        return null
    }

    /** Check-in: la OT pasa a En ejecución y se guardan la hora real y dónde estaba el técnico. */
    suspend fun checkIn(otId: Long, usuarioId: Long, lat: Double, lon: Double): String? =
        if (remoto != null) enServidor { it.checkIn(otId, CheckInRequest(lat, lon)) }
        else aplicar(otId, AccionOT.CHECK_IN, Rol.TECNICO, usuarioId) {
            it.copy(checkInFechaHora = System.currentTimeMillis(), checkInLat = lat, checkInLon = lon)
        }

    /** Check-out: cierra la OT con el diagnóstico, la foto y la hora real de término. */
    suspend fun cerrar(
        otId: Long,
        usuarioId: Long,
        diagnostico: String,
        actividades: String,
        materiales: String,
        fotoUri: String
    ): String? {
        if (remoto != null) {
            return enServidor { it.cierre(otId, CierreRequest(diagnostico, actividades, materiales.ifBlank { null }, fotoUri)) }
        }
        val ahora = System.currentTimeMillis()
        val error = aplicar(otId, AccionOT.CERRAR, Rol.TECNICO, usuarioId) {
            it.copy(
                checkOutFechaHora = ahora,
                diagnostico = diagnostico,
                actividadesRealizadas = actividades,
                materialesUtilizados = materiales.ifBlank { null },
                fotoUri = fotoUri,
                // Si venía de una disconformidad, el motivo ya se corrigió
                motivo = null
            )
        }
        if (error == null) {
            // La OT cerrada es la mantención más reciente del equipo
            val ot = ordenTrabajoDao.getById(otId).first()
            ot?.let { equipoDao.getById(it.equipoId) }?.let { equipoDao.update(it.copy(fechaUltimaMantencion = ahora)) }
        }
        return error
    }
}
