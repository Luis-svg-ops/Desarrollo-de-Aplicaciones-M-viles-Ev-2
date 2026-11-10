package cl.duoc.acis.repository

import cl.duoc.acis.data.EquipoDao
import cl.duoc.acis.data.OrdenTrabajoDao
import cl.duoc.acis.model.Equipo
import kotlinx.coroutines.flow.Flow

class EquipoRepository(
    private val equipoDao: EquipoDao,
    private val ordenTrabajoDao: OrdenTrabajoDao
) {
    fun getByLocal(localId: Long): Flow<List<Equipo>> = equipoDao.getByLocal(localId)

    suspend fun getById(id: Long): Equipo? = equipoDao.getById(id)

    /** Si tiene OT, al "eliminarlo" se da de baja (la pantalla cambia el texto del botón). */
    suspend fun tieneOt(id: Long): Boolean = ordenTrabajoDao.contarPorEquipo(id) > 0

    suspend fun crear(equipo: Equipo): Long = equipoDao.insert(equipo)

    suspend fun actualizar(equipo: Equipo) = equipoDao.update(equipo)

    /**
     * Regla de negocio: si el equipo tiene OT, no se borra, se da de baja para conservar
     * su historial. Si no tiene ninguna, se elimina. Devuelve true si quedó dado de baja.
     */
    suspend fun eliminarODarDeBaja(equipo: Equipo): Boolean {
        return if (ordenTrabajoDao.contarPorEquipo(equipo.id) > 0) {
            equipoDao.update(equipo.copy(dadoDeBaja = true))
            true
        } else {
            equipoDao.delete(equipo)
            false
        }
    }
}
