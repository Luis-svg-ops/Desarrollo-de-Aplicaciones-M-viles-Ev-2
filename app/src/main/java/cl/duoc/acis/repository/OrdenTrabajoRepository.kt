package cl.duoc.acis.repository

import cl.duoc.acis.data.OrdenTrabajoDao
import cl.duoc.acis.model.AccionOT
import cl.duoc.acis.model.MaquinaEstadosOT
import cl.duoc.acis.model.OrdenTrabajo
import cl.duoc.acis.model.OtConDetalle
import cl.duoc.acis.model.Rol
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class OrdenTrabajoRepository(private val ordenTrabajoDao: OrdenTrabajoDao) {

    /** El Jefe ve todas las OT; el Técnico, solo las que tiene asignadas. */
    fun getParaUsuario(usuarioId: Long, rol: Rol): Flow<List<OtConDetalle>> =
        if (rol == Rol.JEFE) ordenTrabajoDao.getAllConDetalle()
        else ordenTrabajoDao.getByTecnicoConDetalle(usuarioId)

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
                motivo = if (nuevoEstado != actual.estado) null else actual.motivo
            )
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
        val cambiada = actual.copy(estado = accion.hacia, motivo = if (accion.pideMotivo) motivo else actual.motivo)
        ordenTrabajoDao.update(datos(cambiada))
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
    ): String? = aplicar(otId, AccionOT.CERRAR, Rol.TECNICO, usuarioId) {
        it.copy(
            checkOutFechaHora = System.currentTimeMillis(),
            diagnostico = diagnostico,
            actividadesRealizadas = actividades,
            materialesUtilizados = materiales.ifBlank { null },
            fotoUri = fotoUri
        )
    }
}
