package cl.duoc.acis.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import cl.duoc.acis.model.Equipo
import cl.duoc.acis.util.fechaHoraCorta
import cl.duoc.acis.view.components.AcisTopBar
import cl.duoc.acis.view.components.TarjetaOt
import cl.duoc.acis.viewmodel.LocalDetalleViewModel

/** Datos del local, sus equipos (el Jefe los administra) y el historial de OT. */
@Composable
fun LocalDetalleScreen(
    viewModel: LocalDetalleViewModel,
    esJefe: Boolean,
    onVolver: () -> Unit,
    onVerOt: (Long) -> Unit,
    onEquipo: (Long?) -> Unit
) {
    val local by viewModel.local.collectAsState()
    val equipos by viewModel.equipos.collectAsState()
    val historial by viewModel.historial.collectAsState()

    Scaffold(topBar = { AcisTopBar(local?.nombre ?: "Local", onVolver = onVolver) }) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            local?.let { l ->
                item {
                    Tarjeta {
                        Fila("Dirección", "${l.direccion}, ${l.comuna}")
                        Fila("Administrador", l.nombreAdministrador)
                        Fila("Teléfono", l.telefono)
                    }
                }
            }

            // ---- Equipos ----
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Equipos (${equipos.size})", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    if (esJefe) {
                        TextButton(onClick = { onEquipo(null) }) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Text(" Agregar")
                        }
                    }
                }
            }
            item {
                Tarjeta {
                    if (equipos.isEmpty()) {
                        Text("Sin equipos registrados", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    equipos.forEachIndexed { i, equipo ->
                        if (i > 0) HorizontalDivider()
                        FilaEquipo(equipo, esJefe, onClick = { onEquipo(equipo.id) })
                    }
                }
            }

            // ---- Historial ----
            item { Text("Historial de OT (${historial.size})", style = MaterialTheme.typography.titleMedium) }
            if (historial.isEmpty()) {
                item { Text("Este local aún no tiene OT", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(historial, key = { it.ot.id }) { item ->
                TarjetaOt(item = item, mostrarTecnico = true, onClick = { onVerOt(item.ot.id) })
            }
        }
    }
}

/** Un equipo de la lista. Solo el Jefe puede tocarlo para editarlo. */
@Composable
private fun FilaEquipo(equipo: Equipo, esJefe: Boolean, onClick: () -> Unit) {
    val base = Modifier.fillMaxWidth()
    Row(
        modifier = (if (esJefe) base.clickable(onClick = onClick) else base).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("${equipo.marca} · ${equipo.modelo}", style = MaterialTheme.typography.bodyLarge)
            Text(
                "${equipo.tipo.texto} · ${equipo.ubicacion.texto} · Última mant.: " +
                        (equipo.fechaUltimaMantencion?.let { fechaHoraCorta(it).substringBefore(" ") } ?: "sin antecedentes"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (esJefe) Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Editar equipo")
    }
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
private fun Tarjeta(contenido: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) { contenido() }
    }
}
