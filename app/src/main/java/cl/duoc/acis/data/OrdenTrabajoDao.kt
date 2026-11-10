package cl.duoc.acis.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import cl.duoc.acis.model.OrdenTrabajo
import kotlinx.coroutines.flow.Flow

@Dao
interface OrdenTrabajoDao {
    // El Jefe ve todas las OT
    @Query("SELECT * FROM ordenes_trabajo ORDER BY fechaProgramada")
    fun getAll(): Flow<List<OrdenTrabajo>>

    // El Técnico solo ve las suyas
    @Query("SELECT * FROM ordenes_trabajo WHERE tecnicoId = :tecnicoId ORDER BY fechaProgramada")
    fun getByTecnico(tecnicoId: Long): Flow<List<OrdenTrabajo>>

    // Historial de un local: de la más reciente a la más antigua
    @Query("SELECT * FROM ordenes_trabajo WHERE localId = :localId ORDER BY fechaProgramada DESC")
    fun getByLocal(localId: Long): Flow<List<OrdenTrabajo>>

    @Query("SELECT * FROM ordenes_trabajo WHERE id = :id LIMIT 1")
    fun getById(id: Long): Flow<OrdenTrabajo?>

    // Para saber si un equipo tiene OT antes de borrarlo
    @Query("SELECT COUNT(*) FROM ordenes_trabajo WHERE equipoId = :equipoId")
    suspend fun contarPorEquipo(equipoId: Long): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(ot: OrdenTrabajo): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(ots: List<OrdenTrabajo>)

    @Update
    suspend fun update(ot: OrdenTrabajo)
}
