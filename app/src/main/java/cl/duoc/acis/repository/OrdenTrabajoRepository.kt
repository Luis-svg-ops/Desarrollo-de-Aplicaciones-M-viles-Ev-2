package cl.duoc.acis.repository

import cl.duoc.acis.data.OrdenTrabajoDao
import cl.duoc.acis.model.EstadoOT
import cl.duoc.acis.model.OrdenTrabajo
import cl.duoc.acis.model.OtConDetalle
import cl.duoc.acis.model.Rol
import kotlinx.coroutines.flow.Flow

class OrdenTrabajoRepository(private val ordenTrabajoDao: OrdenTrabajoDao) {

    /** El Jefe ve todas las OT; el Técnico, solo las que tiene asignadas. */
    fun getParaUsuario(usuarioId: Long, rol: Rol): Flow<List<OtConDetalle>> =
        if (rol == Rol.JEFE) ordenTrabajoDao.getAllConDetalle()
        else ordenTrabajoDao.getByTecnicoConDetalle(usuarioId)

    fun getByLocal(localId: Long): Flow<List<OrdenTrabajo>> = ordenTrabajoDao.getByLocal(localId)

    fun getDetalle(id: Long): Flow<OtConDetalle?> = ordenTrabajoDao.getConDetalle(id)

    suspend fun crear(ot: OrdenTrabajo): Long = ordenTrabajoDao.insert(ot)

    suspend fun actualizar(ot: OrdenTrabajo) = ordenTrabajoDao.update(ot)

    /** El técnico acepta una OT asignada. Solo se puede aceptar si está ASIGNADA. */
    suspend fun aceptar(ot: OrdenTrabajo) {
        if (ot.estado == EstadoOT.ASIGNADA) {
            ordenTrabajoDao.update(ot.copy(estado = EstadoOT.ACEPTADA))
        }
    }
}
