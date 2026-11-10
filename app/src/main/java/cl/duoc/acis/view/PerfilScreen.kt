package cl.duoc.acis.view

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import cl.duoc.acis.model.Rol
import cl.duoc.acis.view.components.AcisTopBar
import cl.duoc.acis.view.components.BotonSecundario
import cl.duoc.acis.view.components.PantallaPendiente

@Composable
fun PerfilScreen(rol: Rol?, onCerrarSesion: () -> Unit) {
    Scaffold(topBar = { AcisTopBar("Perfil") }) { innerPadding ->
        PantallaPendiente(
            titulo = if (rol == Rol.JEFE) "Jefe de mantenimiento" else "Técnico de terreno",
            descripcion = "Datos del usuario, tema oscuro e información de privacidad.",
            hito = 3,
            innerPadding = innerPadding
        ) {
            BotonSecundario("Cerrar sesión", onClick = onCerrarSesion)
        }
    }
}
