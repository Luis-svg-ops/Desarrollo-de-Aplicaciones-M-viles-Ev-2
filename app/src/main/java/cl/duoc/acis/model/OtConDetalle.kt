package cl.duoc.acis.model

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Una OT junto con su local, su equipo y su técnico (como un JOIN en SQL).
 * Room la arma sola a partir de las columnas localId, equipoId y tecnicoId.
 */
data class OtConDetalle(
    @Embedded val ot: OrdenTrabajo,
    @Relation(parentColumn = "localId", entityColumn = "id")
    val local: Local,
    @Relation(parentColumn = "equipoId", entityColumn = "id")
    val equipo: Equipo,
    // Null si la OT todavía no tiene técnico asignado
    @Relation(parentColumn = "tecnicoId", entityColumn = "id")
    val tecnico: Usuario?
)
