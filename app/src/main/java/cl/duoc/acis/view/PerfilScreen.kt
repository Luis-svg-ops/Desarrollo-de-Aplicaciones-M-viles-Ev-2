package cl.duoc.acis.view

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cl.duoc.acis.model.Rol
import cl.duoc.acis.view.components.AcisTopBar
import cl.duoc.acis.viewmodel.PerfilViewModel
import cl.duoc.acis.viewmodel.iniciales

/** Datos del usuario, para qué usa la app cada permiso, y cerrar sesión. */
@Composable
fun PerfilScreen(viewModel: PerfilViewModel, onCerrarSesion: () -> Unit) {
    val usuario by viewModel.usuario.collectAsState()
    val context = LocalContext.current

    Scaffold(topBar = { AcisTopBar("Perfil") }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ---- Datos del usuario ----
            usuario?.let { u ->
                Tarjeta {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(56.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(iniciales(u.nombre), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleLarge)
                        }
                        Column(modifier = Modifier.padding(start = 16.dp)) {
                            Text(u.nombre, style = MaterialTheme.typography.titleLarge)
                            Text(u.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                if (u.rol == Rol.CLIENTE) "Cliente · solo consulta" else u.rol.texto,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // ---- Permisos y privacidad ----
            // El cliente no usa GPS ni cámara: no necesita esta sección
            if (usuario?.rol != Rol.CLIENTE) {
            Text("Permisos y privacidad", style = MaterialTheme.typography.titleMedium)
            Tarjeta {
                Permiso(Icons.Filled.LocationOn, "Ubicación", "Solo al hacer check-in, para registrar dónde llegaste. No te sigue durante el día.")
                Permiso(Icons.Filled.Add, "Cámara", "Solo para la foto de evidencia del equipo. La foto queda dentro de la app, no en tu galería.")
                Permiso(Icons.Filled.Lock, "Tus datos", "Se guardan solo en este teléfono. La app no envía información a ningún servidor.")
                TextButton(onClick = {
                    // Abre los ajustes de la app, donde el usuario puede dar o quitar cada permiso
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                    )
                }) {
                    Icon(Icons.Filled.Settings, contentDescription = null)
                    Text("  Administrar permisos")
                }
            }
            }

            OutlinedButton(
                onClick = onCerrarSesion,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(56.dp)
            ) { Text("Cerrar sesión") }
        }
    }
}

@Composable
private fun Permiso(icono: ImageVector, titulo: String, descripcion: String) {
    Row(modifier = Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
        Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleSmall)
            Text(descripcion, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Tarjeta(contenido: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) { contenido() }
    }
}
