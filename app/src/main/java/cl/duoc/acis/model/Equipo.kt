package cl.duoc.acis.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Equipo de climatización o refrigeración instalado en un local. */
@Entity(
    tableName = "equipos",
    foreignKeys = [ForeignKey(entity = Local::class, parentColumns = ["id"], childColumns = ["localId"])],
    indices = [Index("localId")]
)
data class Equipo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val localId: Long,
    val tipo: TipoEquipo,
    val marca: String,
    val modelo: String,
    val ubicacion: UbicacionEquipo,
    // Fecha en milisegundos. Null si no hay antecedentes (no se inventan fechas)
    val fechaUltimaMantencion: Long? = null,
    // Un equipo con OT no se borra: se da de baja para conservar su historial
    val dadoDeBaja: Boolean = false
)
