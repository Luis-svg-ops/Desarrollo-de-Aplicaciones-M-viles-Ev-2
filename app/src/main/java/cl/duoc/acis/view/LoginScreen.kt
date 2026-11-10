package cl.duoc.acis.view

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cl.duoc.acis.model.Usuario
import cl.duoc.acis.view.components.CampoValidado
import cl.duoc.acis.view.components.InsigniaMadre
import cl.duoc.acis.viewmodel.LoginViewModel

// Fondo hielo azul de la portada: claro arriba (detrás del logo) y azul marino abajo (detrás del texto, para que se lea bien).
private val FondoHielo = Brush.verticalGradient(
    colors = listOf(Color(0xFF7AABCB), Color(0xFF386687), Color(0xFF2A5577))
)

@Composable
fun LoginScreen(viewModel: LoginViewModel, onLoginExitoso: (Usuario) -> Unit) {
    // La pantalla solo observa el estado del ViewModel
    // El texto de los campos se lee directo (estado de Compose); el resto, con collectAsState
    val email = viewModel.email
    val password = viewModel.password
    val emailError by viewModel.emailError.collectAsState()
    val passwordError by viewModel.passwordError.collectAsState()
    val credencialesError by viewModel.credencialesError.collectAsState()
    val isButtonEnabled by viewModel.isButtonEnabled.collectAsState()
    val cargando by viewModel.cargando.collectAsState()
    val verPassword by viewModel.verPassword.collectAsState()

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FondoHielo)
                    .statusBarsPadding()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Animación 6: al abrir la app, el logo aparece creciendo un poco (bienvenida, una sola vez)
                val entrada = remember { Animatable(0f) }
                LaunchedEffect(Unit) { entrada.animateTo(1f, tween(600)) }
                InsigniaMadre(
                    modifier = Modifier.graphicsLayer {
                        alpha = entrada.value
                        scaleX = 0.8f + 0.2f * entrada.value
                        scaleY = 0.8f + 0.2f * entrada.value
                    }
                )
                Spacer(Modifier.height(12.dp))
                Text("ACIS OT Terreno", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                Text("Órdenes de trabajo en terreno", style = MaterialTheme.typography.bodyMedium, color = Color.White)
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CampoValidado(
                    valor = email,
                    onValorChange = { viewModel.onEmailChange(it) },
                    etiqueta = "Correo electrónico",
                    error = emailError,
                    icono = Icons.Filled.Email,
                    tipoTeclado = KeyboardType.Email
                )
                CampoValidado(
                    valor = password,
                    onValorChange = { viewModel.onPasswordChange(it) },
                    etiqueta = "Contraseña",
                    error = passwordError,
                    icono = Icons.Filled.Lock,
                    tipoTeclado = KeyboardType.Password,
                    transformacion = if (verPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    accionFinal = {
                        TextButton(onClick = { viewModel.alternarVerPassword() }) {
                            Text(if (verPassword) "Ocultar" else "Ver")
                        }
                    }
                )

                // Error general: el correo y la contraseña no coinciden con ningún usuario
                if (credencialesError != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(credencialesError!!, color = MaterialTheme.colorScheme.error)
                    }
                }

                // Botón de 56 dp; deshabilitado mientras haya errores o se esté validando
                Button(
                    onClick = { viewModel.iniciarSesion(onLoginExitoso) },
                    enabled = isButtonEnabled && !cargando,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    if (cargando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Ingresar")
                    }
                }

                Text(
                    "Usuarios de prueba: tecnico@acis.cl o jefe@acis.cl · contraseña 123456",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
