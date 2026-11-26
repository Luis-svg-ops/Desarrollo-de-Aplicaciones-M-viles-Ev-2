package cl.duoc.acis.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cl.duoc.acis.data.remote.UsuarioDto
import cl.duoc.acis.view.components.AcisTopBar
import cl.duoc.acis.view.components.CampoValidado
import cl.duoc.acis.view.components.ErrorServidor
import cl.duoc.acis.viewmodel.UsuariosViewModel
import cl.duoc.acis.viewmodel.UsuariosViewModel.EstadoLista

/** Panel del Administrador: usuarios por aprobar, todos los usuarios y crear trabajadores. */
@Composable
fun UsuariosScreen(viewModel: UsuariosViewModel) {
    val estado by viewModel.estado.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.mensaje) {
        viewModel.mensaje?.let {
            snackbar.showSnackbar(it)
            viewModel.mensajeMostrado()
        }
    }

    Scaffold(
        topBar = { AcisTopBar("Usuarios") },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = viewModel::abrirFormulario,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Nuevo usuario") }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = viewModel.soloPendientes, onClick = { viewModel.cambiarFiltro(true) }, label = { Text("Por aprobar") })
                FilterChip(selected = !viewModel.soloPendientes, onClick = { viewModel.cambiarFiltro(false) }, label = { Text("Todos") })
            }
            // El when cubre los 3 estados de la sealed class
            when (val e = estado) {
                is EstadoLista.Cargando -> Row(Modifier.fillMaxWidth().padding(32.dp), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator()
                }
                is EstadoLista.Error -> Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ErrorServidor(e.mensaje)
                    OutlinedButton(onClick = viewModel::cargar) { Text("Reintentar") }
                }
                is EstadoLista.Listo -> if (e.usuarios.isEmpty()) {
                    Text(
                        if (viewModel.soloPendientes) "No hay clientes esperando aprobación" else "No hay usuarios",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(32.dp)
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(e.usuarios, key = { it.id }) { u ->
                            TarjetaUsuario(u, onAprobar = { viewModel.aprobar(u) }, onEliminar = { viewModel.eliminar(u) })
                        }
                    }
                }
            }
        }
    }

    if (viewModel.formularioAbierto) DialogoNuevoUsuario(viewModel)
}

@Composable
private fun TarjetaUsuario(u: UsuarioDto, onAprobar: () -> Unit, onEliminar: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(u.nombre, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    when (u.estado) { "PENDIENTE" -> "Pendiente"; "INACTIVO" -> "Inactivo"; else -> "Activo" },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (u.estado == "ACTIVO") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
            Text("${u.correo} · ${u.rol.texto}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            u.localId?.let { Text("Local $it", style = MaterialTheme.typography.bodySmall) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                when (u.estado) {
                    "PENDIENTE" -> {
                        Button(onClick = onAprobar) { Text("Aprobar") }
                        OutlinedButton(onClick = onEliminar) { Text("Rechazar") }
                    }
                    "ACTIVO" -> OutlinedButton(onClick = onEliminar) { Text("Desactivar") }
                }
            }
        }
    }
}

@Composable
private fun DialogoNuevoUsuario(viewModel: UsuariosViewModel) {
    AlertDialog(
        onDismissRequest = viewModel::cerrarFormulario,
        title = { Text("Nuevo usuario") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Rol", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    viewModel.rolesQueSePuedenCrear.forEach { r ->
                        FilterChip(selected = viewModel.rol == r, onClick = { viewModel.onRol(r) }, label = { Text(r.texto.substringBefore(" ")) })
                    }
                }
                CampoValidado(viewModel.nombre, viewModel::onNombre, "Nombre completo", viewModel.errorNombre, Icons.Filled.Person)
                CampoValidado(viewModel.correo, viewModel::onCorreo, "Correo", viewModel.errorCorreo, Icons.Filled.Email, tipoTeclado = KeyboardType.Email)
                CampoValidado(viewModel.telefono, viewModel::onTelefono, "Teléfono", viewModel.errorTelefono, Icons.Filled.Phone, tipoTeclado = KeyboardType.Phone)
                CampoValidado(viewModel.clave, viewModel::onClave, "Contraseña inicial", viewModel.errorClave, Icons.Filled.Lock, tipoTeclado = KeyboardType.Password, transformacion = PasswordVisualTransformation())
                ErrorServidor(viewModel.errorFormulario)
            }
        },
        confirmButton = { TextButton(onClick = viewModel::crear) { Text("Crear") } },
        dismissButton = { TextButton(onClick = viewModel::cerrarFormulario) { Text("Cancelar") } }
    )
}
