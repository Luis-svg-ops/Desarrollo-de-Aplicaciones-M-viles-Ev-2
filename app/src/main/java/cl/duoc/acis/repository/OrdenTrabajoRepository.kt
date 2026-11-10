package cl.duoc.acis.repository

import cl.duoc.acis.data.OrdenTrabajoDao
import cl.duoc.acis.model.OrdenTrabajo
import cl.duoc.acis.model.Rol
import kotlinx.coroutines.flow.Flow

class OrdenTrabajoRepository(private val ordenTrabajoDao: OrdenTrabajoDao) {

    /** El Jefe ve todas las OT; el Técnico, solo las que tiene asignadas. */
    fun getParaUsuario(usuarioId: Long, rol: Rol): Flow<List<OrdenTrabajo>> =
        if (rol == Rol.JEFE) ordenTrabajoDao.getAll() else ordenTrabajoDao.getByTecnico(usuarioId)

    fun getByLocal(localId: Long): Flow<List<OrdenTrabajo>> = ordenTrabajoDao.getByLocal(localId)

    fun getById(id: Long): Flow<OrdenTrabajo?> = ordenTrabajoDao.getById(id)

    suspend fun crear(ot: OrdenTrabajo): Long = ordenTrabajoDao.insert(ot)

    suspend fun actualizar(ot: OrdenTrabajo) = ordenTrabajoDao.update(ot)
}
