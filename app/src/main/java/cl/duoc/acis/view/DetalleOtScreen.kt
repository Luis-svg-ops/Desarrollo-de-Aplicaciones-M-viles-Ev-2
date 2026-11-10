package cl.duoc.acis.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import cl.duoc.acis.model.AccionOT
import cl.duoc.acis.model.EstadoOT
import cl.duoc.acis.model.OtConDetalle
import cl.duoc.acis.util.fechaHoraCorta
import cl.duoc.acis.view.components.AcisTopBar
import cl.duoc.acis.view.components.EstadoTexto
import cl.duoc.acis.view.components.iconoEstado
import cl.duoc.acis.view.components.LineaProgreso
import cl.duoc.acis.view.components.colorPrioridad
import cl.duoc.acis.viewmodel.DetalleOtViewModel
import cl.duoc.acis.viewmodel.pasoAlcanzado

@Composable
fun DetalleOtScreen(
    viewModel: DetalleOtViewModel,
    onVolver: () -> Unit,
    onEditar: () -> Unit,
    onDiagnostico: () -> Unit
) {
    val detalle by viewModel.detalle.collectAsState()
    val acciones by viewModel.acciones.collectAsState()
    val puedeEditar by viewModel.puedeEditar.collectAsState()
    val accionConMotivo by viewModel.accionConMotivo.collectAsState()
    val motivoError by viewModel.motivoError.collectAsState()
    val mensaje by viewModel.mensaje.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    // Aviso del resultado de cada acción (o el error que devolvió la lógica)
    LaunchedEffect(mensaje) {
        mensaje?.let {
            snackbar.showSnackbar(it)
            viewModel.mensajeMostrado()
        }
    }

    accionConMotivo?.let { accion ->
        DialogoMotivo(
            accion = accion,
            elegido = viewModel.motivo,
            error = motivoError,
            onElegir = viewModel::onMotivoChange,
            onConfirmar = viewModel::confirmarMotivo,
            onCerrar = viewModel::cerrarDialogo
        )
    }

    Scaffold(
        topBar = { AcisTopBar(detalle?.ot?.numero ?: "Detalle de OT", onVolver = onVolver) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { innerPadding ->
        val item = detalle
        if (item == null) {
            // Mientras Room carga la OT
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Encabezado(item)
            if (item.ot.motivo != null) Aviso("Motivo: ${item.ot.motivo}")
            Datos(item)
            if (item.ot.estado == EstadoOT.CERRADA) Cierre(item)
            Acciones(item, acciones, puedeEditar, onAccion = viewModel::onAccion, onEditar = onEditar, onDiagnostico = onDiagnostico)
        }
    }
}

@Composable
private fun Encabezado(item: OtConDetalle) {
    val ot = item.ot
    TarjetaBlanca {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EstadoTexto(ot.estado, modifier = Modifier.weight(1f))
            Text(
                "Prioridad ${ot.prioridad.texto.lowercase()}",
                color = colorPrioridad(ot.prioridad),
                style = MaterialTheme.typography.labelLarge
            )
        }
        Text(item.local.nombre, style = MaterialTheme.typography.titleLarge)
        Text(
            "${item.local.direccion}, ${item.local.comuna}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LineaProgreso(estado = ot.estado, pasoAlcanzado = pasoAlcanzado(ot), modifier = Modifier.padding(top = 12.dp))
        if (ot.estado == EstadoOT.CREADA) {
            Text("Aún sin técnico asignado", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Datos(item: OtConDetalle) {
    val ot = item.ot
    TarjetaBlanca {
        Fila("Tipo", ot.tipo.texto)
        Fila("Programada", fechaHoraCorta(ot.fechaProgramada))
        Fila("Técnico", item.tecnico?.nombre ?: "Sin asignar")
        Fila("Equipo", "${item.equipo.marca} ${item.equipo.modelo}")
        Fila("Tipo de equipo", item.equipo.tipo.texto)
        Fila("Ubicación", item.equipo.ubicacion.texto)
        Fila("Última mant.", item.equipo.fechaUltimaMantencion?.let { fechaHoraCorta(it).substringBefore(" ") } ?: "Sin antecedentes")
        Fila("Contacto", "${item.local.nombreAdministrador} · ${item.local.telefono}")
        Text("Descripción", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
        Text(ot.descripcion, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun Cierre(item: OtConDetalle) {
    val ot = item.ot
    TarjetaBlanca {
        ot.checkInFechaHora?.let { Fila("Check-in", fechaHoraCorta(it)) }
        ot.checkOutFechaHora?.let { Fila("Check-out", fechaHoraCorta(it)) }
        // Duración de la atención: diferencia entre check-out y check-in (base del SLA)
        val inicio = ot.checkInFechaHora
        val fin = ot.checkOutFechaHora
        if (inicio != null && fin != null) Fila("Duración", "${(fin - inicio) / 60000} min")
        ot.diagnostico?.let { Fila("Diagnóstico", it) }
        ot.actividadesRealizadas?.let { Fila("Actividades", it) }
        ot.materialesUtilizados?.let { Fila("Materiales", it) }
    }
}

/**
 * Botones del detalle. No decide nada: muestra las acciones que entregó la máquina de estados.
 */
@Composable
private fun Acciones(
    item: OtConDetalle,
    acciones: List<AccionOT>,
    puedeEditar: Boolean,
    onAccion: (AccionOT) -> Unit,
    onEditar: () -> Unit,
    onDiagnostico: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        acciones.forEach { accion ->
            when (accion) {
                AccionOT.ACEPTAR -> BotonGrande(accion.texto) { onAccion(accion) }
                AccionOT.CHECK_IN -> Aviso("El check-in con GPS se implementa en el hito 6.")
                AccionOT.CERRAR -> BotonGrande(accion.texto, onDiagnostico)
                // Rechazar y cancelar se ven en rojo: sacan a la OT del camino normal
                AccionOT.RECHAZAR, AccionOT.CANCELAR -> BotonContorno(accion.texto, peligro = true) { onAccion(accion) }
                else -> BotonContorno(accion.texto) { onAccion(accion) }
            }
        }
        if (puedeEditar) {
            val texto = when (item.ot.estado) {
                EstadoOT.CREADA -> "Asignar técnico"
                EstadoOT.RECHAZADA, EstadoOT.VISITA_FALLIDA, EstadoOT.POSTERGADA -> "Reasignar o reprogramar"
                else -> "Editar OT"
            }
            BotonContorno(texto, onClick = onEditar)
        }
    }
}

@Composable
private fun BotonContorno(texto: String, peligro: Boolean = false, onClick: () -> Unit) {
    val color = if (peligro) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        modifier = Modifier.fillMaxWidth().height(56.dp)
    ) { Text(texto) }
}

/** Diálogo para elegir el motivo (los motivos vienen de la acción, no de la pantalla). */
@Composable
private fun DialogoMotivo(
    accion: AccionOT,
    elegido: String?,
    error: String?,
    onElegir: (String) -> Unit,
    onConfirmar: () -> Unit,
    onCerrar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCerrar,
        icon = { Icon(iconoEstado(accion.hacia), contentDescription = null) },
        title = { Text(accion.texto) },
        text = {
            Column {
                Text("Selecciona el motivo:", style = MaterialTheme.typography.bodyMedium)
                accion.motivos.forEach { m ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .selectable(selected = m == elegido, onClick = { onElegir(m) }, role = Role.RadioButton)
                    ) {
                        RadioButton(selected = m == elegido, onClick = null)
                        Text(m, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                if (error != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error, modifier = Modifier.padding(end = 4.dp))
                        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onConfirmar) { Text("Confirmar") } },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Volver") } }
    )
}

@Composable
private fun BotonGrande(texto: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text(texto) }
}

@Composable
private fun Fila(etiqueta: String, valor: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            etiqueta,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(120.dp)
        )
        Text(valor, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun Aviso(texto: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Text("  $texto", color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun TarjetaBlanca(contenido: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { contenido() }
    }
}
