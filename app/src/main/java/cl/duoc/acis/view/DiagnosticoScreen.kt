package cl.duoc.acis.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import cl.duoc.acis.util.crearUriFoto
import cl.duoc.acis.util.fechaHoraCorta
import cl.duoc.acis.view.components.AcisTopBar
import cl.duoc.acis.view.components.CampoValidado
import cl.duoc.acis.view.components.FotoEvidencia
import cl.duoc.acis.viewmodel.DiagnosticoViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DiagnosticoScreen(viewModel: DiagnosticoViewModel, onVolver: () -> Unit, onCheckOut: () -> Unit) {
    val ot by viewModel.ot.collectAsState()
    val diagnosticoError by viewModel.diagnosticoError.collectAsState()
    val actividadesError by viewModel.actividadesError.collectAsState()
    val fotoError by viewModel.fotoError.collectAsState()
    val isButtonEnabled by viewModel.isButtonEnabled.collectAsState()
    val cargando by viewModel.cargando.collectAsState()
    val cerrada by viewModel.cerrada.collectAsState()
    val errorGuardar by viewModel.errorGuardar.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    // ---- Cámara ----
    // Dónde guardará la cámara la foto (rememberSaveable: sobrevive si Android cierra la app mientras la cámara está abierta)
    var uriPendiente by rememberSaveable { mutableStateOf<String?>(null) }
    var explicarCamara by remember { mutableStateOf(false) }
    var permisoNegado by remember { mutableStateOf(false) }

    // Abre la app de cámara; devuelve true si se tomó la foto
    val tomarFoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = uriPendiente
        if (ok && uri != null) viewModel.onFotoTomada(uri) else viewModel.onFotoCancelada()
    }
    val abrirCamara = {
        val uri = crearUriFoto(context)
        uriPendiente = uri.toString()
        tomarFoto.launch(uri)
    }
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) abrirCamara() else permisoNegado = true
    }
    val onTomarFoto = {
        val tienePermiso = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (tienePermiso) abrirCamara() else explicarCamara = true
    }

    if (explicarCamara) {
        // Primero se explica para qué se usa; recién después se pide el permiso real
        AlertDialog(
            onDismissRequest = { explicarCamara = false },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            title = { Text("Tomar foto") },
            text = { Text("Se abrirá la cámara. La foto se guarda dentro de la app, no en tu galería. Fotografía solo el equipo.") },
            confirmButton = {
                TextButton(onClick = {
                    explicarCamara = false
                    pedirPermiso.launch(Manifest.permission.CAMERA)
                }) { Text("Continuar") }
            },
            dismissButton = { TextButton(onClick = { explicarCamara = false }) { Text("Ahora no") } }
        )
    }

    if (permisoNegado) {
        AlertDialog(
            onDismissRequest = { permisoNegado = false },
            icon = { Icon(Icons.Filled.Warning, contentDescription = null) },
            title = { Text("Sin permiso de cámara") },
            text = { Text("La foto del equipo es obligatoria para cerrar la OT. Puedes activar la cámara en Ajustes > Permisos > Cámara.") },
            confirmButton = {
                TextButton(onClick = {
                    permisoNegado = false
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                    )
                }) { Text("Abrir ajustes") }
            },
            dismissButton = { TextButton(onClick = { permisoNegado = false }) { Text("Cerrar") } }
        )
    }

    // Tras el check-out: aviso y vuelta a la lista
    LaunchedEffect(cerrada) {
        if (cerrada) {
            launch { snackbar.showSnackbar("OT cerrada · check-out registrado") }
            delay(1200)
            onCheckOut()
        }
    }
    LaunchedEffect(errorGuardar) {
        errorGuardar?.let {
            snackbar.showSnackbar(it)
            viewModel.errorMostrado()
        }
    }

    Scaffold(
        topBar = { AcisTopBar("Diagnóstico · ${ot?.numero ?: ""}", onVolver = onVolver) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ot?.checkInFechaHora?.let {
                Text(
                    "En atención desde: ${fechaHoraCorta(it)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            CampoValidado(
                valor = viewModel.diagnostico,
                onValorChange = viewModel::onDiagnosticoChange,
                etiqueta = "Diagnóstico *",
                error = diagnosticoError,
                icono = Icons.Filled.Build,
                ayuda = "${viewModel.diagnostico.length}/500",
                lineas = 3
            )
            CampoValidado(
                valor = viewModel.actividades,
                onValorChange = viewModel::onActividadesChange,
                etiqueta = "Actividades realizadas *",
                error = actividadesError,
                icono = Icons.Filled.List,
                ayuda = "${viewModel.actividades.length}/500",
                lineas = 3
            )
            CampoValidado(
                valor = viewModel.materiales,
                onValorChange = viewModel::onMaterialesChange,
                etiqueta = "Materiales utilizados",
                error = null,
                icono = Icons.Filled.Edit,
                ayuda = "Opcional"
            )

            Text("Foto de evidencia *", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
            val foto = viewModel.fotoUri
            if (foto != null) FotoEvidencia(foto)
            OutlinedButton(onClick = onTomarFoto, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(if (foto == null) "  Tomar foto" else "  Tomar otra foto")
            }
            fotoError?.let {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }

            Button(
                onClick = viewModel::hacerCheckOut,
                enabled = isButtonEnabled && !cargando,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(56.dp)
            ) {
                if (cargando) CircularProgressIndicator(modifier = Modifier.size(24.dp)) else Text("Hacer check-out y cerrar OT")
            }
        }
    }
}
