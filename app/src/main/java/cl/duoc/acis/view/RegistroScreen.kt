package cl.duoc.acis.view

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import cl.duoc.acis.view.components.AcisTopBar
import cl.duoc.acis.view.components.BotonCargando
import cl.duoc.acis.view.components.CampoValidado
import cl.duoc.acis.view.components.ErrorServidor
import cl.duoc.acis.view.components.MensajeExito
import cl.duoc.acis.viewmodel.RegistroViewModel

/** Registro de clientes con el código de su local. La cuenta queda pendiente hasta que el admin la apruebe. */
@Composable
fun RegistroScreen(viewModel: RegistroViewModel, onVolver: () -> Unit) {
    Scaffold(topBar = { AcisTopBar("Crear cuenta de cliente", onVolver = onVolver) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val listo = viewModel.listo
            if (listo != null) {
                // Registro enviado: no se puede entrar hasta la aprobación
                MensajeExito(listo)
                OutlinedButton(onClick = onVolver, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) { Text("Volver al inicio de sesión") }
                return@Column
            }
            Text(
                "Pide a ACIS el código de tu local. Un administrador revisará tu registro antes de que puedas entrar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            CampoValidado(viewModel.nombre, viewModel::onNombre, "Nombre completo", viewModel.errorNombre, Icons.Filled.Person)
            CampoValidado(viewModel.correo, viewModel::onCorreo, "Correo electrónico", viewModel.errorCorreo, Icons.Filled.Email, tipoTeclado = KeyboardType.Email)
            CampoValidado(viewModel.telefono, viewModel::onTelefono, "Teléfono", viewModel.errorTelefono, Icons.Filled.Phone, tipoTeclado = KeyboardType.Phone, ayuda = "Ej.: +56 9 1234 5678")
            CampoValidado(viewModel.codigoLocal, viewModel::onCodigo, "Código del local", viewModel.errorCodigo, Icons.Filled.Home, ayuda = "Ej.: MALL-02")
            CampoValidado(viewModel.clave, viewModel::onClave, "Contraseña", viewModel.errorClave, Icons.Filled.Lock, tipoTeclado = KeyboardType.Password, transformacion = PasswordVisualTransformation(), ayuda = "Mínimo 6 caracteres")
            CampoValidado(viewModel.confirmacion, viewModel::onConfirmacion, "Repetir contraseña", viewModel.errorConfirmacion, Icons.Filled.Lock, tipoTeclado = KeyboardType.Password, transformacion = PasswordVisualTransformation())
            ErrorServidor(viewModel.errorServidor)
            BotonCargando("Crear cuenta", viewModel.cargando, viewModel::registrar)
        }
    }
}
