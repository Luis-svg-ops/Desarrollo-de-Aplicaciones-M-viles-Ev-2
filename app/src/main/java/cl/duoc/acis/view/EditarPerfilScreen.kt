package cl.duoc.acis.view

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import cl.duoc.acis.view.components.AcisTopBar
import cl.duoc.acis.view.components.BotonCargando
import cl.duoc.acis.view.components.CampoValidado
import cl.duoc.acis.view.components.ErrorServidor
import cl.duoc.acis.viewmodel.EditarPerfilViewModel

@Composable
fun EditarPerfilScreen(viewModel: EditarPerfilViewModel, onVolver: () -> Unit) {
    // Al guardar bien, se vuelve al perfil
    LaunchedEffect(viewModel.guardado) { if (viewModel.guardado) onVolver() }

    Scaffold(topBar = { AcisTopBar("Editar perfil", onVolver = onVolver) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CampoValidado(viewModel.nombre, viewModel::onNombre, "Nombre completo", viewModel.errorNombre, Icons.Filled.Person)
            CampoValidado(viewModel.telefono, viewModel::onTelefono, "Teléfono", viewModel.errorTelefono, Icons.Filled.Phone, tipoTeclado = KeyboardType.Phone, ayuda = "Ej.: +56 9 1234 5678")

            Text("Cambiar contraseña (opcional)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
            CampoValidado(viewModel.claveActual, viewModel::onClaveActual, "Contraseña actual", viewModel.errorClaveActual, Icons.Filled.Lock, tipoTeclado = KeyboardType.Password, transformacion = PasswordVisualTransformation())
            CampoValidado(viewModel.claveNueva, viewModel::onClaveNueva, "Contraseña nueva", viewModel.errorClaveNueva, Icons.Filled.Lock, tipoTeclado = KeyboardType.Password, transformacion = PasswordVisualTransformation())
            CampoValidado(viewModel.confirmacion, viewModel::onConfirmacion, "Repetir contraseña nueva", viewModel.errorConfirmacion, Icons.Filled.Lock, tipoTeclado = KeyboardType.Password, transformacion = PasswordVisualTransformation())

            ErrorServidor(viewModel.errorServidor)
            BotonCargando("Guardar cambios", viewModel.cargando, viewModel::guardar)
        }
    }
}
