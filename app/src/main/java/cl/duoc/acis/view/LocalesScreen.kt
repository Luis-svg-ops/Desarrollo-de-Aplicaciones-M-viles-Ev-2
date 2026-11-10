package cl.duoc.acis.view

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import cl.duoc.acis.view.components.AcisTopBar
import cl.duoc.acis.view.components.BotonPrincipal
import cl.duoc.acis.view.components.PantallaPendiente

@Composable
fun LocalesScreen(esJefe: Boolean, onVerLocal: (Long) -> Unit) {
    Scaffold(topBar = { AcisTopBar(if (esJefe) "Locales" else "Historial por local") }) { innerPadding ->
        PantallaPendiente(
            titulo = if (esJefe) "Locales" else "Historial por local",
            descripcion = "Lista de locales con su cantidad de equipos y de OT.",
            hito = 8,
            innerPadding = innerPadding
        ) {
            BotonPrincipal("Ver un local de ejemplo", onClick = { onVerLocal(1L) })
        }
    }
}
