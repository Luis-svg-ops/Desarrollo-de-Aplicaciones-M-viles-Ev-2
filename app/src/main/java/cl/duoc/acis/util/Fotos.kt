package cl.duoc.acis.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Crea un archivo vacío en la carpeta privada de la app (files/fotos) y devuelve su dirección
 * content:// para que la app de cámara guarde ahí la foto. La foto no queda en la galería.
 */
fun crearUriFoto(context: Context): Uri {
    val carpeta = File(context.filesDir, "fotos").apply { mkdirs() }
    val archivo = File(carpeta, "ot_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fotos", archivo)
}

/**
 * Carga la foto reducida (lado mayor cercano a maxLado px) para no gastar memoria:
 * una foto de cámara puede pesar 12 MP y solo se muestra en una tarjeta de 200 dp.
 */
fun cargarMiniatura(context: Context, uri: Uri, maxLado: Int = 1024): Bitmap? = try {
    val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, medidas) }
    var escala = 1
    while (maxOf(medidas.outWidth, medidas.outHeight) / (escala * 2) >= maxLado) escala *= 2
    val opciones = BitmapFactory.Options().apply { inSampleSize = escala }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) }
} catch (e: Exception) {
    null // archivo borrado o dañado: se muestra el aviso en vez de la foto
}
