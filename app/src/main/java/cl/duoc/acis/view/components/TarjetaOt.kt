package cl.duoc.acis.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.duoc.acis.model.OtConDetalle
import cl.duoc.acis.model.UbicacionEquipo
import cl.duoc.acis.util.fechaHoraCorta

/**
 * Tarjeta de una OT en la lista: franja de color a la izquierda según la prioridad
 * (rojo alta, amarillo media, verde baja), la prioridad también escrita y el estado como texto con ícono.
 */
@Composable
fun TarjetaOt(item: OtConDetalle, mostrarTecnico: Boolean, onClick: () -> Unit, paraCliente: Boolean = false) {
    val ot = item.ot
    val colorFranja = colorPrioridad(ot.prioridad)
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(6.dp).fillMaxHeight().background(colorFranja))
            Column(
                modifier = Modifier.weight(1f).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${ot.numero} · ${ot.tipo.texto}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    // La prioridad también va escrita: no depender solo del color (accesibilidad)
                    Text(
                        "Prioridad ${ot.prioridad.texto.lowercase()}",
                        style = MaterialTheme.typography.labelMedium,
                        color = colorFranja,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(item.local.nombre, style = MaterialTheme.typography.titleMedium)
                val exterior = if (item.equipo.ubicacion == UbicacionEquipo.EXTERIOR) " · Exterior" else ""
                Text(
                    "${item.equipo.marca} ${item.equipo.modelo} · ${item.equipo.tipo.texto}$exterior",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                    val tecnico = if (mostrarTecnico) " · ${item.tecnico?.nombre ?: "Sin técnico"}" else ""
                    Text(
                        fechaHoraCorta(ot.fechaProgramada) + tecnico,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    EstadoTexto(ot.estado, paraCliente = paraCliente)
                }
            }
        }
    }
}
