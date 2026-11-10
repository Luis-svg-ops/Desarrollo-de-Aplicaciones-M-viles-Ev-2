package cl.duoc.acis.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import cl.duoc.acis.model.Equipo
import kotlinx.coroutines.flow.Flow

@Dao
interface EquipoDao {
    // Los equipos dados de baja no aparecen en la lista ni al crear OT
    @Query("SELECT * FROM equipos WHERE localId = :localId AND dadoDeBaja = 0 ORDER BY marca")
    fun getByLocal(localId: Long): Flow<List<Equipo>>

    @Query("SELECT * FROM equipos WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Equipo?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(equipo: Equipo): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(equipos: List<Equipo>)

    @Update
    suspend fun update(equipo: Equipo)

    @Delete
    suspend fun delete(equipo: Equipo)
}
