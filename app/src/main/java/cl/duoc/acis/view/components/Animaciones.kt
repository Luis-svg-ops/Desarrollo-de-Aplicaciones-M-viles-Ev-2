package cl.duoc.acis.view.components

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically

/**
 * Duraciones y transiciones compartidas: así todas las animaciones de la app se sienten iguales.
 * Son cortas (200-300 ms) para que guíen la vista sin hacer esperar al técnico en terreno.
 */
const val DURACION_CORTA = 200
const val DURACION_NORMAL = 300

/** Para tarjetas o avisos que aparecen (por ejemplo, "Check-in registrado"): se despliegan hacia abajo. */
val aparecer: EnterTransition = fadeIn(tween(DURACION_NORMAL)) + expandVertically(tween(DURACION_NORMAL))
val desaparecer: ExitTransition = fadeOut(tween(DURACION_CORTA)) + shrinkVertically(tween(DURACION_CORTA))
