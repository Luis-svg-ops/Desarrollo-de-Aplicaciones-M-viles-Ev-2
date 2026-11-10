package cl.duoc.acis.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.duoc.acis.model.TipoEquipo
import cl.duoc.acis.model.UbicacionEquipo
import cl.duoc.acis.view.components.AcisTopBar
import cl.duoc.acis.view.components.CampoClicable
import cl.duoc.acis.view.components.CampoValidado
import cl.duoc.acis.view.components.Opciones
import cl.duoc.acis.viewmodel.EquipoFormViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val FORMATO_FECHA = DateTimeFormatter.ofPattern("dd-MM-yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquipoFormScreen(viewModel: EquipoFormViewModel, onVolver: () -> Unit) {
    val marcaError by viewModel.marcaError.collectAsState()
    val modeloError by viewModel.modeloError.collectAsState()
    val fechaError by viewModel.fechaError.collectAsState()
    val isButtonEnabled by viewModel.isButtonEnabled.collectAsState()
    val tieneOt by viewModel.tieneOt.collectAsState()
    val terminado by viewModel.terminado.collectAsState()
    var mostrarFecha by remember { mutableStateOf(false) }
    var confirmarBorrar by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    // Al guardar o borrar: aviso y vuelta al local
    LaunchedEffect(terminado) {
        terminado?.let {
            launch { snackbar.showSnackbar(it) }
            delay(1200)
            onVolver()
        }
    }

    Scaffold(
        topBar = { AcisTopBar(if (viewModel.esEdicion) "Editar equipo" else "Nuevo equipo", onVolver = onVolver) },
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
            Text("Tipo", style = MaterialTheme.typography.labelLarge)
            Opciones(TipoEquipo.entries.toList(), viewModel.tipo, { it.texto }) { viewModel.onTipoChange(it) }

            CampoValidado(
                valor = viewModel.marca,
                onValorChange = viewModel::onMarcaChange,
                etiqueta = "Marca *",
                error = marcaError,
                icono = Icons.Filled.Build
            )
            CampoValidado(
                valor = viewModel.modelo,
                onValorChange = viewModel::onModeloChange,
                etiqueta = "Modelo *",
                error = modeloError,
                icono = Icons.Filled.Edit
            )

            Text("Ubicación", style = MaterialTheme.typography.labelLarge)
            Opciones(UbicacionEquipo.entries.toList(), viewModel.ubicacion, { it.texto }) { viewModel.onUbicacionChange(it) }

            CampoClicable(
                valor = viewModel.ultimaMantencion?.format(FORMATO_FECHA) ?: "",
                etiqueta = "Última mantención",
                error = fechaError,
                icono = Icons.Filled.DateRange,
                onClick = { mostrarFecha = true },
                ayuda = "Déjala vacía si no hay antecedentes",
                accionFinal = if (viewModel.ultimaMantencion != null) {
                    { IconButton(onClick = { viewModel.onFechaChange(null) }) { Icon(Icons.Filled.Clear, contentDescription = "Borrar fecha") } }
                } else null
            )

            Button(
                onClick = viewModel::guardar,
                enabled = isButtonEnabled,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(56.dp)
            ) { Text("Guardar equipo") }

            if (viewModel.esEdicion) {
                OutlinedButton(
                    onClick = { confirmarBorrar = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) { Text(if (tieneOt) "Dar de baja equipo" else "Eliminar equipo") }
            }
        }
    }

    if (confirmarBorrar) {
        // Acción irreversible: siempre se confirma, y se explica qué pasará
        AlertDialog(
            onDismissRequest = { confirmarBorrar = false },
            icon = { Icon(Icons.Filled.Delete, contentDescription = null) },
            title = { Text(if (tieneOt) "¿Dar de baja el equipo?" else "¿Eliminar el equipo?") },
            text = {
                Text(
                    if (tieneOt) "El equipo tiene OT registradas: no se borra, sale de la lista y su historial se conserva."
                    else "El equipo no tiene OT. Se eliminará y no se puede deshacer."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmarBorrar = false
                    viewModel.eliminar()
                }) { Text(if (tieneOt) "Dar de baja" else "Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmarBorrar = false }) { Text("Volver") } }
        )
    }

    if (mostrarFecha) {
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = viewModel.ultimaMantencion?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
            // La última mantención ya pasó: no se pueden elegir días futuros
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isAfter(LocalDate.now())
            }
        )
        DatePickerDialog(
            onDismissRequest = { mostrarFecha = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let {
                        viewModel.onFechaChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    mostrarFecha = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { mostrarFecha = false }) { Text("Cancelar") } }
        ) { DatePicker(state = estado) }
    }
}
