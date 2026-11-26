package cl.duoc.acis.repository

import cl.duoc.acis.data.LocalDao
import cl.duoc.acis.data.EquipoDao
import cl.duoc.acis.data.remote.OrdenesApi
import cl.duoc.acis.data.remote.Resultado
import cl.duoc.acis.data.remote.aEntidad
import cl.duoc.acis.data.remote.llamarApi
import cl.duoc.acis.model.Local
import kotlinx.coroutines.flow.Flow

class LocalRepository(
    private val localDao: LocalDao,
    private val equipoDao: EquipoDao? = null,
    private val remoto: OrdenesApi? = null
) {

    /** Trae locales y sus equipos desde ms-ordenes (solo jefe y admin tienen permiso). */
    suspend fun sincronizar() {
        val api = remoto ?: return
        val r = llamarApi { api.locales() }
        if (r !is Resultado.Exito) return
        localDao.upsertAll(r.datos.map { it.aEntidad() })
        r.datos.forEach { local ->
            val equipos = llamarApi { api.equipos(local.id) }
            if (equipos is Resultado.Exito) equipoDao?.upsertAll(equipos.datos.map { it.aEntidad() })
        }
    }


    fun getAll(): Flow<List<Local>> = localDao.getAll()

    fun getById(id: Long): Flow<Local?> = localDao.getById(id)

    suspend fun actualizar(local: Local) = localDao.update(local)
}
