package cl.duoc.acis.view.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Fila de chips de 48 dp para elegir una opción (tipo, prioridad, ubicación...). */
@Composable
fun <T> Opciones(opciones: List<T>, seleccion: T, texto: (T) -> String, onElegir: (T) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        opciones.forEach { opcion ->
            FilterChip(
                selected = opcion == seleccion,
                onClick = { onElegir(opcion) },
                label = { Text(texto(opcion)) },
                leadingIcon = if (opcion == seleccion) {
                    { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else null,
                modifier = Modifier.height(48.dp)
            )
        }
    }
}
