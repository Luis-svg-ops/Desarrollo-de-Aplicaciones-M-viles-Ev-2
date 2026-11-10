package cl.duoc.acis.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cl.duoc.acis.model.EventoOT
import kotlinx.coroutines.flow.Flow

@Dao
interface EventoDao {

    // Del más antiguo al más reciente, como una línea de tiempo
    @Query("SELECT * FROM eventos_ot WHERE otId = :otId ORDER BY fecha, id")
    fun getByOt(otId: Long): Flow<List<EventoOT>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(evento: EventoOT)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(eventos: List<EventoOT>)
}
