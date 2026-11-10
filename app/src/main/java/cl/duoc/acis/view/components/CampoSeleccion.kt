package cl.duoc.acis.view.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Campo que no se escribe: se toca para elegir (fecha, hora o una lista).
 * Usa el mismo CampoValidado, así que muestra los errores igual que los demás campos.
 */
@Composable
fun CampoClicable(
    valor: String,
    etiqueta: String,
    error: String?,
    icono: ImageVector,
    onClick: () -> Unit,
    habilitado: Boolean = true,
    ayuda: String? = null,
    accionFinal: @Composable (() -> Unit)? = null
) {
    Box {
        CampoValidado(
            valor = valor,
            onValorChange = {},
            etiqueta = etiqueta,
            error = error,
            icono = icono,
            soloLectura = true,
            habilitado = habilitado,
            ayuda = ayuda,
            accionFinal = accionFinal
        )
        // Capa transparente encima del campo: el toque abre el selector
        if (habilitado) {
            Box(Modifier.matchParentSize().clickable(onClick = onClick))
        }
    }
}

/**
 * Lista desplegable con validación. Cada opción es (id, texto); el id puede ser null
 * para una opción como "Sin asignar".
 */
@Composable
fun CampoSeleccion(
    etiqueta: String,
    opciones: List<Pair<Long?, String>>,
    seleccionId: Long?,
    onSeleccion: (Long?) -> Unit,
    error: String?,
    icono: ImageVector,
    habilitado: Boolean = true,
    ayuda: String? = null
) {
    var abierto by remember { mutableStateOf(false) }
    val textoSeleccionado = opciones.firstOrNull { it.first == seleccionId }?.second ?: ""
    Box {
        CampoClicable(
            valor = textoSeleccionado,
            etiqueta = etiqueta,
            error = error,
            icono = icono,
            onClick = { abierto = true },
            habilitado = habilitado,
            ayuda = ayuda,
            accionFinal = { Icon(Icons.Filled.ArrowDropDown, contentDescription = "Abrir lista") }
        )
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            opciones.forEach { (id, texto) ->
                DropdownMenuItem(
                    text = { Text(texto) },
                    onClick = {
                        onSeleccion(id)
                        abierto = false
                    }
                )
            }
        }
    }
}
