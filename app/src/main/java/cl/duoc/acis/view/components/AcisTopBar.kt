package cl.duoc.acis.view.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import cl.duoc.acis.ui.theme.LocalAcisColores

/**
 * Barra superior de la app. Si recibe onVolver, muestra la flecha atrás
 * (pantallas internas); si no, es una pantalla principal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcisTopBar(titulo: String, onVolver: (() -> Unit)? = null) {
    val colores = LocalAcisColores.current
    TopAppBar(
        title = { Text(titulo) },
        navigationIcon = {
            if (onVolver != null) {
                IconButton(onClick = onVolver) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colores.barra,
            titleContentColor = colores.onBarra,
            navigationIconContentColor = colores.onBarra,
            actionIconContentColor = colores.onBarra
        )
    )
}
