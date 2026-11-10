package cl.duoc.acis.view

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import cl.duoc.acis.view.components.AcisTopBar
import cl.duoc.acis.view.components.BotonPrincipal
import cl.duoc.acis.view.components.PantallaPendiente

@Composable
fun ListaOtScreen(esJefe: Boolean, onVerOt: (Long) -> Unit, onNuevaOt: () -> Unit) {
    Scaffold(
        topBar = { AcisTopBar(if (esJefe) "Órdenes de trabajo" else "Mis OT del día") },
        floatingActionButton = {
            // Solo el Jefe crea OT
            if (esJefe) {
                ExtendedFloatingActionButton(
                    onClick = onNuevaOt,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Nueva OT") },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            }
        }
    ) { innerPadding ->
        PantallaPendiente(
            titulo = "Lista de OT",
            descripcion = if (esJefe) "Todas las OT con chips de filtro deslizables."
            else "Tarjeta de próxima visita y pestañas Pendientes, En ejecución y Cerradas.",
            hito = 4,
            innerPadding = innerPadding
        ) {
            BotonPrincipal("Ver detalle de una OT de ejemplo", onClick = { onVerOt(1L) })
        }
    }
}
