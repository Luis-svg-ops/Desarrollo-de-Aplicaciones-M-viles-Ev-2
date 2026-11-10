package cl.duoc.acis.repository

import cl.duoc.acis.data.EquipoDao
import cl.duoc.acis.data.EventoDao
import cl.duoc.acis.data.UsuarioDao
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
    private val usuarioDao: UsuarioDao
) {

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
        Rol.JEFE -> ordenTrabajoDao.getAllConDetalle()
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
        aplicar(otId, AccionOT.CHECK_IN, Rol.TECNICO, usuarioId) {
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
