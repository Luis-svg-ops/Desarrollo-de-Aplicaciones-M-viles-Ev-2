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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.duoc.acis.model.EstadoOT
import cl.duoc.acis.model.OtConDetalle
import cl.duoc.acis.util.fechaHoraCorta
import cl.duoc.acis.view.components.AcisTopBar
import cl.duoc.acis.view.components.EstadoTexto
import cl.duoc.acis.view.components.LineaProgreso
import cl.duoc.acis.view.components.colorPrioridad
import cl.duoc.acis.viewmodel.DetalleOtViewModel
import cl.duoc.acis.viewmodel.pasoAlcanzado

@Composable
fun DetalleOtScreen(
    viewModel: DetalleOtViewModel,
    esJefe: Boolean,
    onVolver: () -> Unit,
    onEditar: () -> Unit,
    onDiagnostico: () -> Unit
) {
    val detalle by viewModel.detalle.collectAsState()

    Scaffold(topBar = { AcisTopBar(detalle?.ot?.numero ?: "Detalle de OT", onVolver = onVolver) }) { innerPadding ->
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
            Acciones(item, esJefe, onAceptar = { viewModel.aceptar() }, onEditar = onEditar, onDiagnostico = onDiagnostico)
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

/** Botones según el rol y el estado de la OT. */
@Composable
private fun Acciones(
    item: OtConDetalle,
    esJefe: Boolean,
    onAceptar: () -> Unit,
    onEditar: () -> Unit,
    onDiagnostico: () -> Unit
) {
    val estado = item.ot.estado
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!esJefe) {
            when (estado) {
                EstadoOT.ASIGNADA -> BotonGrande("Aceptar OT", onAceptar)
                EstadoOT.ACEPTADA -> Aviso("El check-in con GPS se implementa en el hito 6.")
                EstadoOT.EN_EJECUCION -> BotonGrande("Registrar diagnóstico y cerrar", onDiagnostico)
                else -> Unit
            }
        } else if (estado != EstadoOT.CERRADA && estado != EstadoOT.CANCELADA && estado != EstadoOT.EN_EJECUCION) {
            OutlinedButton(onClick = onEditar, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Editar OT") }
        }
    }
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
