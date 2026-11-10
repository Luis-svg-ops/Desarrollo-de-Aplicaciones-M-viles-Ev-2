package cl.duoc.acis.repository

import cl.duoc.acis.data.LocalDao
import cl.duoc.acis.model.Local
import kotlinx.coroutines.flow.Flow

class LocalRepository(private val localDao: LocalDao) {

    fun getAll(): Flow<List<Local>> = localDao.getAll()

    fun getById(id: Long): Flow<Local?> = localDao.getById(id)

    suspend fun actualizar(local: Local) = localDao.update(local)
}
