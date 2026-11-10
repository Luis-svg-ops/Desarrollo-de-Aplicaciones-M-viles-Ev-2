package cl.duoc.acis.view

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import cl.duoc.acis.view.components.AcisTopBar
import cl.duoc.acis.view.components.BotonPrincipal
import cl.duoc.acis.view.components.PantallaPendiente

@Composable
fun DiagnosticoScreen(otId: Long, onVolver: () -> Unit, onCheckOut: () -> Unit) {
    Scaffold(topBar = { AcisTopBar("Diagnóstico · OT #$otId", onVolver = onVolver) }) { innerPadding ->
        PantallaPendiente(
            titulo = "Diagnóstico y check-out",
            descripcion = "Diagnóstico, actividades, materiales, foto de evidencia obligatoria y botón de check-out.",
            hito = 7,
            innerPadding = innerPadding
        ) {
            BotonPrincipal("Probar check-out", onClick = onCheckOut)
        }
    }
}
