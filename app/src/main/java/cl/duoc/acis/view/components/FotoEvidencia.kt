package cl.duoc.acis.view.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cl.duoc.acis.util.cargarMiniatura
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Foto de evidencia ajustada a la interfaz: ancho completo, 200 dp de alto, recortada
 * (ContentScale.Crop) y con bordes redondeados. Se carga en segundo plano para no trabar la pantalla.
 */
@Composable
fun FotoEvidencia(uri: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val imagen by produceState<ImageBitmap?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) { cargarMiniatura(context, Uri.parse(uri))?.asImageBitmap() }
    }
    val forma = RoundedCornerShape(12.dp)
    val base = modifier.fillMaxWidth().height(200.dp).clip(forma)
    val foto = imagen
    if (foto != null) {
        Image(bitmap = foto, contentDescription = "Foto de evidencia del equipo", contentScale = ContentScale.Crop, modifier = base)
    } else {
        Box(base.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            Text("Cargando foto…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
