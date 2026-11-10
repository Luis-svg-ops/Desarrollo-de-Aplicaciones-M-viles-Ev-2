package cl.duoc.acis.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Un cambio en la vida de una OT ("Aceptada por Pedro Soto", "Disconformidad: Trabajo incompleto").
 * Con esto el jefe y el cliente ven qué pasó y cuándo, no solo el estado actual.
 */
@Entity(
    tableName = "eventos_ot",
    foreignKeys = [ForeignKey(entity = OrdenTrabajo::class, parentColumns = ["id"], childColumns = ["otId"])],
    indices = [Index("otId")]
)
data class EventoOT(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val otId: Long,
    // Estado en que quedó la OT con este cambio (para el ícono y el color)
    val estado: EstadoOT,
    val descripcion: String,
    val fecha: Long
)
