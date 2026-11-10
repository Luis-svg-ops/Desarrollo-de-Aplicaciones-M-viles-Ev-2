package cl.duoc.acis.view

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import cl.duoc.acis.view.components.AcisTopBar
import cl.duoc.acis.view.components.BotonPrincipal
import cl.duoc.acis.view.components.PantallaPendiente

@Composable
fun DetalleOtScreen(
    otId: Long,
    esJefe: Boolean,
    onVolver: () -> Unit,
    onEditar: () -> Unit,
    onDiagnostico: () -> Unit
) {
    Scaffold(topBar = { AcisTopBar("OT #$otId", onVolver = onVolver) }) { innerPadding ->
        PantallaPendiente(
            titulo = "Detalle de la OT",
            descripcion = "Datos de la OT, línea de progreso, insignias del equipo y acciones según rol y estado.",
            hito = 4,
            innerPadding = innerPadding
        ) {
            if (esJefe) BotonPrincipal("Editar OT", onClick = onEditar)
            else BotonPrincipal("Registrar diagnóstico", onClick = onDiagnostico)
        }
    }
}
