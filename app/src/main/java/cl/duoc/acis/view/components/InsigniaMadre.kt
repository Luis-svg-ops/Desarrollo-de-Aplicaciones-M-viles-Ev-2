package cl.duoc.acis.view.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cl.duoc.acis.R

/**
 * Insignia madre de la app: el logo de ACIS OT Terreno (escudo con copo de nieve y engranaje).
 * Es una imagen con fondo transparente (res/drawable-nodpi/logo_acis.png), así que se ve bien
 * sobre el fondo hielo azul de la portada. El tamaño indica el alto de referencia; el ancho
 * se calcula solo para no deformar el logo.
 */
@Composable
fun InsigniaMadre(modifier: Modifier = Modifier, tamano: Dp = 84.dp) {
    Image(
        painter = painterResource(R.drawable.logo_acis),
        contentDescription = "Logo de ACIS OT Terreno",
        modifier = modifier.height(tamano * 1.4f),
        contentScale = ContentScale.Fit
    )
}

@Preview
@Composable
private fun InsigniaMadrePreview() {
    InsigniaMadre()
}
