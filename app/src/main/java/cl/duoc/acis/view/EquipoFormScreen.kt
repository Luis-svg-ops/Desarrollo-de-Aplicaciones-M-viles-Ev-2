package cl.duoc.acis.view

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import cl.duoc.acis.view.components.AcisTopBar
import cl.duoc.acis.view.components.PantallaPendiente

@Composable
fun EquipoFormScreen(localId: Long, equipoId: Long?, onVolver: () -> Unit) {
    Scaffold(topBar = { AcisTopBar(if (equipoId == null) "Nuevo equipo" else "Editar equipo", onVolver = onVolver) }) { innerPadding ->
        PantallaPendiente(
            titulo = "Equipo del local #$localId",
            descripcion = "Tipo, marca, modelo, ubicación y última mantención, con vista previa de las insignias. Dar de baja si tiene OT.",
            hito = 8,
            innerPadding = innerPadding
        )
    }
}
