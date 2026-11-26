package cl.duoc.acis.view.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Mensaje del servidor bajo un formulario (ícono + texto rojo). Aparece con animación. */
@Composable
fun ErrorServidor(mensaje: String?) {
    AnimatedVisibility(visible = mensaje != null, enter = aparecer, exit = desaparecer) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text(mensaje ?: "", color = MaterialTheme.colorScheme.error)
        }
    }
}

/** Mensaje de éxito (ícono verde + texto). */
@Composable
fun MensajeExito(mensaje: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.size(8.dp))
        Text(mensaje, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Botón principal de 56 dp que muestra un círculo de carga mientras espera al servidor. */
@Composable
fun BotonCargando(texto: String, cargando: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = !cargando, modifier = Modifier.fillMaxWidth().height(56.dp)) {
        if (cargando) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
        } else {
            Text(texto)
        }
    }
}
