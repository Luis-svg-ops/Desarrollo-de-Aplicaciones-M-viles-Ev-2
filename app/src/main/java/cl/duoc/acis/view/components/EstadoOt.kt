package cl.duoc.acis.view.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.duoc.acis.model.EstadoOT
import cl.duoc.acis.model.Prioridad
import cl.duoc.acis.ui.theme.PrioridadAlta
import cl.duoc.acis.ui.theme.PrioridadBaja
import cl.duoc.acis.ui.theme.PrioridadMedia

/** Ícono de cada estado (todos están en material-icons-core). */
fun iconoEstado(estado: EstadoOT): ImageVector = when (estado) {
    EstadoOT.CREADA -> Icons.Filled.Create
    EstadoOT.ASIGNADA -> Icons.Filled.Person
    EstadoOT.ACEPTADA -> Icons.Filled.ThumbUp
    EstadoOT.RECHAZADA -> Icons.Filled.Close
    EstadoOT.EN_EJECUCION -> Icons.Filled.Build
    EstadoOT.CERRADA -> Icons.Filled.CheckCircle
    EstadoOT.CONFORME -> Icons.Filled.Star
    EstadoOT.POSTERGADA -> Icons.Filled.DateRange
    EstadoOT.VISITA_FALLIDA -> Icons.Filled.Warning
    EstadoOT.CANCELADA -> Icons.Filled.Clear
}

@Composable
fun colorEstado(estado: EstadoOT): Color = when (estado) {
    EstadoOT.CERRADA, EstadoOT.CONFORME -> PrioridadBaja
    EstadoOT.EN_EJECUCION, EstadoOT.POSTERGADA -> MaterialTheme.colorScheme.secondary
    EstadoOT.RECHAZADA, EstadoOT.VISITA_FALLIDA -> MaterialTheme.colorScheme.error
    EstadoOT.ASIGNADA, EstadoOT.ACEPTADA -> MaterialTheme.colorScheme.primary
    EstadoOT.CREADA, EstadoOT.CANCELADA -> MaterialTheme.colorScheme.onSurfaceVariant
}

fun colorPrioridad(prioridad: Prioridad): Color = when (prioridad) {
    Prioridad.ALTA -> PrioridadAlta
    Prioridad.MEDIA -> PrioridadMedia
    Prioridad.BAJA -> PrioridadBaja
}

/** El estado como texto con ícono, sin recuadro (decisión del mockup). */
@Composable
fun EstadoTexto(estado: EstadoOT, modifier: Modifier = Modifier, paraCliente: Boolean = false) {
    // Animación 4: al cambiar el estado (aceptar, check-in...) el color se transforma y el texto nuevo
    // sube mientras el anterior sale, para que el técnico note que su acción tuvo efecto
    val color by animateColorAsState(colorEstado(estado), tween(DURACION_NORMAL), label = "colorEstado")
    AnimatedContent(
        targetState = estado,
        transitionSpec = {
            (fadeIn(tween(DURACION_NORMAL)) + slideInVertically(tween(DURACION_NORMAL)) { it / 2 })
                .togetherWith(fadeOut(tween(DURACION_CORTA)))
        },
        modifier = modifier,
        label = "estado"
    ) { actual ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(iconoEstado(actual), contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Text(textoEstado(actual, paraCliente), color = color, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
        }
    }
}

/**
 * Para el cliente, una OT cerrada por el técnico todavía está "En revisión" por la jefatura:
 * recién al dar la conformidad pasa a "Conforme".
 */
fun textoEstado(estado: EstadoOT, paraCliente: Boolean): String =
    if (paraCliente && estado == EstadoOT.CERRADA) "En revisión" else estado.texto
