package cl.duoc.acis.view

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import cl.duoc.acis.view.components.AcisTopBar
import cl.duoc.acis.view.components.PantallaPendiente

@Composable
fun OtFormScreen(otId: Long?, onVolver: () -> Unit) {
    Scaffold(topBar = { AcisTopBar(if (otId == null) "Nueva OT" else "Editar OT", onVolver = onVolver) }) { innerPadding ->
        PantallaPendiente(
            titulo = if (otId == null) "Crear OT" else "Editar OT",
            descripcion = "Local, equipo filtrado por local, técnico, tipo, prioridad, fecha, hora y descripción, con validación por campo.",
            hito = 5,
            innerPadding = innerPadding
        )
    }
}
