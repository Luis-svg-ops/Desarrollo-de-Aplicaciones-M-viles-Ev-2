package cl.duoc.acis.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Local comercial de un cliente (supermercado, mall, tienda). */
@Entity(tableName = "locales")
data class Local(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val direccion: String,
    val comuna: String,
    val nombreAdministrador: String,
    val telefono: String
)
