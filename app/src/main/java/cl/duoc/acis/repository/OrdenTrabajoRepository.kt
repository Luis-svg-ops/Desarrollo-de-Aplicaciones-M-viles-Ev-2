package cl.duoc.acis.repository

import cl.duoc.acis.data.OrdenTrabajoDao
import cl.duoc.acis.model.EstadoOT
import cl.duoc.acis.model.OrdenTrabajo
import cl.duoc.acis.model.OtConDetalle
import cl.duoc.acis.model.Rol
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class OrdenTrabajoRepository(private val ordenTrabajoDao: OrdenTrabajoDao) {

    /** El Jefe ve todas las OT; el Técnico, solo las que tiene asignadas. */
    fun getParaUsuario(usuarioId: Long, rol: Rol): Flow<List<OtConDetalle>> =
        if (rol == Rol.JEFE) ordenTrabajoDao.getAllConDetalle()
        else ordenTrabajoDao.getByTecnicoConDetalle(usuarioId)

    fun getByLocal(localId: Long): Flow<List<OrdenTrabajo>> = ordenTrabajoDao.getByLocal(localId)

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

    suspend fun actualizar(ot: OrdenTrabajo) = ordenTrabajoDao.update(ot)

    /** El técnico acepta una OT asignada. Solo se puede aceptar si está ASIGNADA. */
    suspend fun aceptar(ot: OrdenTrabajo) {
        if (ot.estado == EstadoOT.ASIGNADA) {
            ordenTrabajoDao.update(ot.copy(estado = EstadoOT.ACEPTADA))
        }
    }
}
