package cl.duoc.acis.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cl.duoc.acis.model.EstadoOT

// Pasos del camino normal de una OT
private val PASOS = listOf(EstadoOT.ASIGNADA, EstadoOT.ACEPTADA, EstadoOT.EN_EJECUCION, EstadoOT.CERRADA)

private enum class TipoPaso { HECHO, ACTUAL, PENDIENTE, ALERTA }

/**
 * Línea de progreso: Asignada → Aceptada → En ejecución → Cerrada.
 * Si la OT salió del camino (visita fallida, postergada, rechazada o cancelada),
 * el paso siguiente al último alcanzado se marca en rojo con el estado real.
 * pasoAlcanzado: índice del último paso normal al que llegó la OT (-1 si ninguno).
 */
@Composable
fun LineaProgreso(estado: EstadoOT, pasoAlcanzado: Int, modifier: Modifier = Modifier) {
    val fueraDelCamino = estado !in PASOS && estado != EstadoOT.CREADA
    val actual = PASOS.indexOf(estado)

    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        PASOS.forEachIndexed { i, paso ->
            val tipo = when {
                fueraDelCamino && i == pasoAlcanzado + 1 -> TipoPaso.ALERTA
                fueraDelCamino && i <= pasoAlcanzado -> TipoPaso.HECHO
                i < actual || (estado == EstadoOT.CERRADA && i == actual) -> TipoPaso.HECHO
                i == actual -> TipoPaso.ACTUAL
                else -> TipoPaso.PENDIENTE
            }
            val texto = if (tipo == TipoPaso.ALERTA) estado.texto else paso.texto
            Paso(numero = i + 1, texto = texto, tipo = tipo, icono = iconoEstado(if (tipo == TipoPaso.ALERTA) estado else paso),
                modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun Paso(numero: Int, texto: String, tipo: TipoPaso, icono: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    val primario = MaterialTheme.colorScheme.primary
    val (fondo, borde, contenido) = when (tipo) {
        TipoPaso.HECHO -> Triple(primario, primario, MaterialTheme.colorScheme.onPrimary)
        TipoPaso.ACTUAL -> Triple(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.secondary)
        TipoPaso.ALERTA -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.error)
        TipoPaso.PENDIENTE -> Triple(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.outline, MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(fondo)
                .border(2.dp, borde, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            when (tipo) {
                TipoPaso.HECHO -> Icon(Icons.Filled.Check, contentDescription = null, tint = contenido, modifier = Modifier.size(18.dp))
                TipoPaso.PENDIENTE -> Text("$numero", color = contenido, style = MaterialTheme.typography.labelMedium)
                else -> Icon(icono, contentDescription = null, tint = contenido, modifier = Modifier.size(18.dp))
            }
        }
        Text(
            texto,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = if (tipo == TipoPaso.PENDIENTE) MaterialTheme.colorScheme.onSurfaceVariant
            else if (tipo == TipoPaso.ALERTA) MaterialTheme.colorScheme.error else Color.Unspecified,
            modifier = Modifier.padding(horizontal = 2.dp).height(32.dp)
        )
    }
}
