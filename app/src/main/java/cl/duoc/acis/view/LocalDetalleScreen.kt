package cl.duoc.acis.view

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import cl.duoc.acis.view.components.AcisTopBar
import cl.duoc.acis.view.components.BotonPrincipal
import cl.duoc.acis.view.components.BotonSecundario
import cl.duoc.acis.view.components.PantallaPendiente

@Composable
fun LocalDetalleScreen(
    localId: Long,
    esJefe: Boolean,
    onVolver: () -> Unit,
    onVerOt: (Long) -> Unit,
    onEquipo: (Long?) -> Unit
) {
    Scaffold(topBar = { AcisTopBar("Local #$localId", onVolver = onVolver) }) { innerPadding ->
        PantallaPendiente(
            titulo = "Equipos e historial del local",
            descripcion = "Datos del local, sus equipos con insignias de tipo y el historial de OT.",
            hito = 8,
            innerPadding = innerPadding
        ) {
            BotonPrincipal("Ver una OT del historial", onClick = { onVerOt(4L) })
            // Solo el Jefe administra equipos
            if (esJefe) BotonSecundario("Agregar equipo", onClick = { onEquipo(null) })
        }
    }
}
