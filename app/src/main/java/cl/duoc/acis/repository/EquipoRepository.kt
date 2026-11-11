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
     * Reglas de negocio al borrar un equipo:
     * - si tiene OT en curso (no terminadas), no se puede: el técnico podría ir en camino;
     * - si solo tiene OT terminadas, no se borra: se da de baja para conservar su historial;
     * - si no tiene ninguna OT, se elimina.
     */
    suspend fun eliminarODarDeBaja(equipo: Equipo): ResultadoBorrado = when {
        ordenTrabajoDao.contarAbiertasPorEquipo(equipo.id) > 0 -> ResultadoBorrado.TIENE_OT_EN_CURSO
        ordenTrabajoDao.contarPorEquipo(equipo.id) > 0 -> {
            equipoDao.update(equipo.copy(dadoDeBaja = true))
            ResultadoBorrado.DADO_DE_BAJA
        }
        else -> {
            equipoDao.delete(equipo)
            ResultadoBorrado.ELIMINADO
        }
    }
}

/** Qué pasó al intentar borrar un equipo. */
enum class ResultadoBorrado { ELIMINADO, DADO_DE_BAJA, TIENE_OT_EN_CURSO }
