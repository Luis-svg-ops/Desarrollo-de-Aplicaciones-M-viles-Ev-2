package cl.duoc.acis.view

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import cl.duoc.acis.viewmodel.RecuperarViewModel

/** Recuperar contraseña en 2 pasos. El cambio de paso es animado (AnimatedContent). */
@Composable
fun RecuperarScreen(viewModel: RecuperarViewModel, onVolver: () -> Unit) {
    Scaffold(topBar = { AcisTopBar("Recuperar contraseña", onVolver = onVolver) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val listo = viewModel.listo
            if (listo != null) {
                MensajeExito(listo)
                OutlinedButton(onClick = onVolver, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) { Text("Ir a iniciar sesión") }
                return@Column
            }
            AnimatedContent(targetState = viewModel.paso, label = "paso") { paso ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (paso == 1) {
                        Text("Paso 1 de 2 · Te enviaremos un código de 6 dígitos a tu correo.", style = MaterialTheme.typography.bodyMedium)
                        CampoValidado(viewModel.correo, viewModel::onCorreo, "Correo electrónico", viewModel.errorCorreo, Icons.Filled.Email, tipoTeclado = KeyboardType.Email)
                        ErrorServidor(viewModel.errorServidor)
                        BotonCargando("Enviar código", viewModel.cargando, viewModel::pedirCodigo)
                    } else {
                        Text("Paso 2 de 2 · ${viewModel.info ?: ""}", style = MaterialTheme.typography.bodyMedium)
                        Text("El código vence en 10 minutos y tienes 5 intentos.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        CampoValidado(viewModel.codigo, viewModel::onCodigo, "Código de 6 dígitos", viewModel.errorCodigo, Icons.Filled.Star, tipoTeclado = KeyboardType.Number)
                        CampoValidado(viewModel.clave, viewModel::onClave, "Contraseña nueva", viewModel.errorClave, Icons.Filled.Lock, tipoTeclado = KeyboardType.Password, transformacion = PasswordVisualTransformation())
                        CampoValidado(viewModel.confirmacion, viewModel::onConfirmacion, "Repetir contraseña nueva", viewModel.errorConfirmacion, Icons.Filled.Lock, tipoTeclado = KeyboardType.Password, transformacion = PasswordVisualTransformation())
                        ErrorServidor(viewModel.errorServidor)
                        BotonCargando("Cambiar contraseña", viewModel.cargando, viewModel::restablecer)
                        TextButton(onClick = viewModel::volverAPedir) { Text("Pedir un código nuevo") }
                    }
                }
            }
        }
    }
}
