package cl.duoc.acis.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import cl.duoc.acis.model.Local
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalDao {
    @Query("SELECT * FROM locales ORDER BY nombre")
    fun getAll(): Flow<List<Local>>

    @Query("SELECT * FROM locales WHERE id = :id LIMIT 1")
    fun getById(id: Long): Flow<Local?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(locales: List<Local>)

    @Update
    suspend fun update(local: Local)
}
