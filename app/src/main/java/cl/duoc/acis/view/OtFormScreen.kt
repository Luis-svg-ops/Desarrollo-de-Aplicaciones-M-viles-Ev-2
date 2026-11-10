package cl.duoc.acis.view

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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cl.duoc.acis.model.Prioridad
import cl.duoc.acis.model.TipoOT
import cl.duoc.acis.view.components.AcisTopBar
import cl.duoc.acis.view.components.CampoClicable
import cl.duoc.acis.view.components.CampoSeleccion
import cl.duoc.acis.view.components.CampoValidado
import cl.duoc.acis.viewmodel.OtFormViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val FORMATO_FECHA = DateTimeFormatter.ofPattern("dd-MM-yyyy")
private val FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtFormScreen(viewModel: OtFormViewModel, onVolver: () -> Unit) {
    val locales by viewModel.locales.collectAsState()
    val equipos by viewModel.equipos.collectAsState()
    val tecnicos by viewModel.tecnicos.collectAsState()
    val localError by viewModel.localError.collectAsState()
    val equipoError by viewModel.equipoError.collectAsState()
    val fechaError by viewModel.fechaError.collectAsState()
    val descripcionError by viewModel.descripcionError.collectAsState()
    val isButtonEnabled by viewModel.isButtonEnabled.collectAsState()
    val cargando by viewModel.cargando.collectAsState()
    val guardado by viewModel.guardado.collectAsState()

    var mostrarFecha by remember { mutableStateOf(false) }
    var mostrarHora by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    // Al guardar: aviso de confirmación y vuelta a la pantalla anterior
    LaunchedEffect(guardado) {
        if (guardado) {
            val mensaje = if (viewModel.esEdicion) "Cambios guardados" else "OT creada"
            launch { snackbar.showSnackbar(mensaje) }
            delay(1200)
            onVolver()
        }
    }

    Scaffold(
        topBar = { AcisTopBar(if (viewModel.esEdicion) "Editar OT" else "Nueva OT", onVolver = onVolver) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CampoSeleccion(
                etiqueta = "Local *",
                opciones = locales.map { it.id to it.nombre },
                seleccionId = viewModel.localId,
                onSeleccion = { viewModel.onLocalChange(it) },
                error = localError,
                icono = Icons.Filled.Place
            )
            // El equipo se filtra por el local elegido; sin local no se puede elegir
            CampoSeleccion(
                etiqueta = "Equipo *",
                opciones = equipos.map { it.id to "${it.marca} · ${it.modelo}" },
                seleccionId = viewModel.equipoId,
                onSeleccion = { viewModel.onEquipoChange(it) },
                error = equipoError,
                icono = Icons.Filled.Build,
                habilitado = viewModel.localId != null,
                ayuda = if (viewModel.localId == null) "Primero elige el local" else null
            )
            CampoSeleccion(
                etiqueta = "Técnico",
                opciones = listOf<Pair<Long?, String>>(null to "Sin asignar por ahora") + tecnicos.map { it.id to it.nombre },
                seleccionId = viewModel.tecnicoId,
                onSeleccion = { viewModel.onTecnicoChange(it) },
                error = null,
                icono = Icons.Filled.Person,
                ayuda = "Opcional: si no eliges técnico, la OT queda Creada"
            )

            Text("Tipo", style = MaterialTheme.typography.labelLarge)
            Opciones(TipoOT.entries.toList(), viewModel.tipo, { it.texto }) { viewModel.onTipoChange(it) }
            Text("Prioridad", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
            Opciones(Prioridad.entries.toList(), viewModel.prioridad, { it.texto }) { viewModel.onPrioridadChange(it) }

            // Fecha y hora una debajo de la otra; cada una abre su propio diálogo
            CampoClicable(
                valor = viewModel.fecha?.format(FORMATO_FECHA) ?: "",
                etiqueta = "Fecha programada *",
                error = fechaError,
                icono = Icons.Filled.DateRange,
                onClick = { mostrarFecha = true }
            )
            CampoClicable(
                valor = viewModel.hora.format(FORMATO_HORA),
                etiqueta = "Hora",
                error = null,
                icono = Icons.Filled.DateRange,
                onClick = { mostrarHora = true }
            )
            CampoValidado(
                valor = viewModel.descripcion,
                onValorChange = { viewModel.onDescripcionChange(it) },
                etiqueta = "Descripción del trabajo *",
                error = descripcionError,
                icono = Icons.Filled.Edit,
                tipoTeclado = KeyboardType.Text,
                ayuda = "${viewModel.descripcion.length}/300",
                lineas = 3
            )

            Button(
                onClick = { viewModel.guardar() },
                enabled = isButtonEnabled && !cargando,
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(top = 4.dp)
            ) {
                if (cargando) CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                else Text(if (viewModel.esEdicion) "Guardar cambios" else "Crear OT")
            }
            if (!isButtonEnabled) {
                Text(
                    "Completa los campos obligatorios (*) para guardar",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (mostrarFecha) {
        // El calendario no deja elegir días pasados (la validación también lo revisa)
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = viewModel.fecha?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isBefore(LocalDate.now())
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

    if (mostrarHora) {
        val estado = rememberTimePickerState(initialHour = viewModel.hora.hour, initialMinute = viewModel.hora.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { mostrarHora = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onHoraChange(LocalTime.of(estado.hour, estado.minute))
                    mostrarHora = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { mostrarHora = false }) { Text("Cancelar") } },
            text = { TimePicker(state = estado) }
        )
    }
}

/** Fila de chips de 48 dp para elegir una opción (tipo o prioridad). */
@Composable
private fun <T> Opciones(opciones: List<T>, seleccion: T, texto: (T) -> String, onElegir: (T) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        opciones.forEach { opcion ->
            FilterChip(
                selected = opcion == seleccion,
                onClick = { onElegir(opcion) },
                label = { Text(texto(opcion)) },
                leadingIcon = if (opcion == seleccion) {
                    { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else null,
                modifier = Modifier.height(48.dp)
            )
        }
    }
}
