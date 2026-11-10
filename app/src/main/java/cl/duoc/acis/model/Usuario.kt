package cl.duoc.acis.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// El correo no se puede repetir: es con lo que se inicia sesión
@Entity(tableName = "usuarios", indices = [Index(value = ["email"], unique = true)])
data class Usuario(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val email: String,
    // Solo es una demo local: la contraseña va en texto plano (se indica en el README)
    val password: String,
    val rol: Rol,
    // Solo para el cliente: el local que administra (ve únicamente las OT de ese local)
    val localId: Long? = null
)
