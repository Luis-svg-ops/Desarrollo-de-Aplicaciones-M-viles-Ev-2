package cl.duoc.acis.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cl.duoc.acis.model.EstadoOT
import cl.duoc.acis.model.OtConDetalle
import cl.duoc.acis.util.fechaHoraCorta
import cl.duoc.acis.view.components.AcisTopBar
import cl.duoc.acis.view.components.TarjetaOt
import cl.duoc.acis.viewmodel.ListaOtViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaOtScreen(
    viewModel: ListaOtViewModel,
    esJefe: Boolean,
    onVerOt: (Long) -> Unit,
    onNuevaOt: () -> Unit,
    // El cliente ve pestañas como el técnico, pero sin la tarjeta de próxima visita (es del técnico)
    esCliente: Boolean = false
) {
    val ots by viewModel.otsFiltradas.collectAsState()
    val conteos by viewModel.conteos.collectAsState()
    val filtroSeleccionado by viewModel.filtroSeleccionado.collectAsState()
    val proximaVisita by viewModel.proximaVisita.collectAsState()

    Scaffold(
        topBar = { AcisTopBar(if (esJefe) "Órdenes de trabajo" else if (esCliente) "OT de mi local" else "Mis OT del día") },
        floatingActionButton = {
            // Solo el Jefe crea OT
            if (esJefe) {
                ExtendedFloatingActionButton(
                    onClick = onNuevaOt,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Nueva OT") },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (!esJefe) {
                // Técnico: pestañas a todo el ancho
                PrimaryTabRow(selectedTabIndex = filtroSeleccionado) {
                    viewModel.filtros.forEachIndexed { i, filtro ->
                        Tab(
                            selected = filtroSeleccionado == i,
                            onClick = { viewModel.seleccionarFiltro(i) },
                            text = { Text("${filtro.texto} (${conteos.getOrElse(i) { 0 }})") }
                        )
                    }
                }
            } else {
                // Jefe: chips deslizables de 48 dp
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(viewModel.filtros) { i, filtro ->
                        FilterChip(
                            selected = filtroSeleccionado == i,
                            onClick = { viewModel.seleccionarFiltro(i) },
                            label = { Text("${filtro.texto} · ${conteos.getOrElse(i) { 0 }}") },
                            leadingIcon = if (filtroSeleccionado == i) {
                                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else null,
                            modifier = Modifier.height(48.dp)
                        )
                    }
                }
            }

            LazyColumn(
                // Espacio extra abajo para que el botón "Nueva OT" no tape la última tarjeta
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = if (esJefe) 96.dp else 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!esJefe && !esCliente) {
                    item { ProximaVisita(proximaVisita, onVerOt) }
                }
                if (ots.isEmpty()) {
                    item {
                        Text(
                            "No hay OT en este filtro",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(32.dp)
                        )
                    }
                }
                items(ots, key = { it.ot.id }) { item ->
                    // Animación 2: al cambiar de filtro, las tarjetas entran, salen y se reordenan suavemente
                    // (por eso cada tarjeta tiene una key: así Compose sabe cuál es cuál)
                    Box(Modifier.animateItem()) {
                        TarjetaOt(item = item, mostrarTecnico = esJefe || esCliente, onClick = { onVerOt(item.ot.id) }, paraCliente = esCliente)
                    }
                }
            }
        }
    }
}

/** Tarjeta destacada con la visita en curso o la siguiente del día. */
@Composable
private fun ProximaVisita(item: OtConDetalle?, onVerOt: (Long) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(6.dp).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
            Column(modifier = Modifier.weight(1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (item == null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("  Jornada al día: no tienes más visitas hoy", style = MaterialTheme.typography.titleSmall)
                    }
                } else {
                    val enCurso = item.ot.estado == EstadoOT.EN_EJECUCION
                    Text(
                        if (enCurso) "VISITA EN CURSO" else "PRÓXIMA VISITA",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(item.local.nombre, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${item.local.direccion}, ${item.local.comuna}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${fechaHoraCorta(item.ot.fechaProgramada)} · ${item.equipo.modelo} · Prioridad ${item.ot.prioridad.texto.lowercase()}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    // El botón cambia según el estado: una OT asignada primero se acepta
                    val textoBoton = when (item.ot.estado) {
                        EstadoOT.ASIGNADA -> "Revisar y aceptar"
                        EstadoOT.ACEPTADA -> "Ver detalle para hacer check-in"
                        else -> "Continuar la visita"
                    }
                    Button(
                        onClick = { onVerOt(item.ot.id) },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(56.dp)
                    ) { Text(textoBoton) }
                }
            }
        }
    }
}
