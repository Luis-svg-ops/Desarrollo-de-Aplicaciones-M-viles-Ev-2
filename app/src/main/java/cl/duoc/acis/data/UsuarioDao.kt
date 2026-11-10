package cl.duoc.acis.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cl.duoc.acis.model.Usuario
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {
    @Query("SELECT * FROM usuarios WHERE rol = 'TECNICO' ORDER BY nombre")
    fun getTecnicos(): Flow<List<Usuario>>

    // Para el login
    @Query("SELECT * FROM usuarios WHERE email = :email LIMIT 1")
    suspend fun getByEmail(email: String): Usuario?

    @Query("SELECT * FROM usuarios WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Usuario?

    // ABORT: si el correo ya existe, falla en vez de pisar el usuario anterior
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(usuarios: List<Usuario>)
}
