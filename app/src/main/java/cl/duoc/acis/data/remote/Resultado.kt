package cl.duoc.acis.data.remote

import com.google.gson.Gson
import retrofit2.HttpException
import java.io.IOException

/**
 * Resultado de una llamada al servidor. Es una "sealed class": solo puede ser Exito o Error,
 * y el compilador obliga a manejar los dos casos con un when (igual que CategoryUiState del ejemplo del profe).
 */
sealed class Resultado<out T> {
    data class Exito<T>(val datos: T) : Resultado<T>()
    data class Error(val mensaje: String, val sinConexion: Boolean = false) : Resultado<Nothing>()
}

/** Mensajes para el usuario (se usan también en las pruebas). */
object MensajesRed {
    const val SIN_CONEXION = "Sin conexión con el servidor. Revisa tu internet e intenta de nuevo"
    const val SESION_EXPIRADA = "Tu sesión expiró. Vuelve a iniciar sesión"
    const val DESCONOCIDO = "Ocurrió un error inesperado. Intenta de nuevo"
}

private val gson = Gson()

/**
 * Ejecuta una llamada a la API y convierte cualquier excepción en un mensaje claro:
 * - IOException: no hay red o el servidor está apagado.
 * - HttpException: el servidor respondió con error (400, 401, 403, 409...). Se muestra su mensaje.
 * - Cualquier otra: error inesperado (por ejemplo, un JSON que no se pudo leer).
 */
suspend fun <T> llamarApi(llamada: suspend () -> T): Resultado<T> = try {
    Resultado.Exito(llamada())
} catch (e: IOException) {
    Resultado.Error(MensajesRed.SIN_CONEXION, sinConexion = true)
} catch (e: HttpException) {
    Resultado.Error(mensajeDeError(e))
} catch (e: Exception) {
    Resultado.Error(MensajesRed.DESCONOCIDO)
}

/** Lee el campo "error" del cuerpo; un 401 con sesión iniciada significa que el token venció. */
internal fun mensajeDeError(e: HttpException): String {
    val delServidor = try {
        e.response()?.errorBody()?.string()?.let { gson.fromJson(it, ErrorDto::class.java)?.error }
    } catch (ex: Exception) {
        null
    }
    return when {
        !delServidor.isNullOrBlank() -> delServidor
        e.code() == 401 -> MensajesRed.SESION_EXPIRADA
        e.code() == 403 -> "No tienes permiso para esta acción"
        else -> MensajesRed.DESCONOCIDO
    }
}
