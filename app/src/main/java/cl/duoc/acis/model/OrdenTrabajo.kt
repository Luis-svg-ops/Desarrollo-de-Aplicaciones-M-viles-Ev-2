package cl.duoc.acis.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Orden de trabajo (OT): una visita para mantener o reparar un equipo. */
@Entity(
    tableName = "ordenes_trabajo",
    foreignKeys = [
        ForeignKey(entity = Local::class, parentColumns = ["id"], childColumns = ["localId"]),
        ForeignKey(entity = Equipo::class, parentColumns = ["id"], childColumns = ["equipoId"]),
        ForeignKey(entity = Usuario::class, parentColumns = ["id"], childColumns = ["tecnicoId"])
    ],
    indices = [Index("localId"), Index("equipoId"), Index("tecnicoId")]
)
data class OrdenTrabajo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val numero: String,
    val localId: Long,
    val equipoId: Long,
    // Null mientras la OT no tenga técnico asignado
    val tecnicoId: Long? = null,
    val tipo: TipoOT,
    val prioridad: Prioridad,
    val estado: EstadoOT,
    val descripcion: String,
    // Fechas en milisegundos: se guarda la hora real de cada acción (base para el SLA)
    val fechaCreacion: Long,
    val fechaProgramada: Long,
    // Cuándo el técnico aceptó la OT (null si aún no la acepta o se reasignó)
    val fechaAceptacion: Long? = null,
    val checkInFechaHora: Long? = null,
    val checkInLat: Double? = null,
    val checkInLon: Double? = null,
    val checkOutFechaHora: Long? = null,
    val diagnostico: String? = null,
    val actividadesRealizadas: String? = null,
    val materialesUtilizados: String? = null,
    val fotoUri: String? = null,
    // Motivo de rechazo, postergación, visita fallida o cancelación
    val motivo: String? = null,
    // "Eléctrica" para mostrar la insignia de falla eléctrica
    val falla: String? = null
)
