package cl.duoc.acis.data.remote

import android.content.Context

/**
 * A qué servidor se conecta la app. Se elige en el login (⚙ Servidor) y se guarda en el teléfono.
 * - NUBE: los microservicios publicados en internet (para probar en un celular real).
 * - EMULADOR: el backend encendido en el PC (10.0.2.2 = el computador visto desde el emulador).
 * - PERSONALIZADO: otra dirección, por ejemplo un túnel https hacia el PC.
 */
object ConfigServidor {

    enum class Destino(val texto: String) {
        NUBE("En internet (celular)"),
        EMULADOR("PC local (emulador)"),
        PERSONALIZADO("Otra dirección")
    }

    const val NUBE_AUTH = "https://acis-ot-auth.onrender.com/"
    const val NUBE_ORDENES = "https://acis-ot-ordenes.onrender.com/"
    const val EMULADOR_AUTH = "http://10.0.2.2:8090/"
    const val EMULADOR_ORDENES = "http://10.0.2.2:8091/"

    private const val PREFS = "servidor"

    @Volatile var destino: Destino = Destino.NUBE
        private set
    @Volatile var authPersonalizada: String = ""
        private set
    @Volatile var ordenesPersonalizada: String = ""
        private set

    val urlAuth: String
        get() = when (destino) {
            Destino.NUBE -> NUBE_AUTH
            Destino.EMULADOR -> EMULADOR_AUTH
            Destino.PERSONALIZADO -> conBarra(authPersonalizada)
        }

    val urlOrdenes: String
        get() = when (destino) {
            Destino.NUBE -> NUBE_ORDENES
            Destino.EMULADOR -> EMULADOR_ORDENES
            Destino.PERSONALIZADO -> conBarra(ordenesPersonalizada)
        }

    /** Lee la elección guardada (se llama al abrir la app). */
    fun cargar(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        destino = runCatching { Destino.valueOf(p.getString("destino", null) ?: "") }.getOrDefault(Destino.NUBE)
        authPersonalizada = p.getString("auth", "") ?: ""
        ordenesPersonalizada = p.getString("ordenes", "") ?: ""
    }

    fun guardar(context: Context, nuevo: Destino, auth: String = "", ordenes: String = "") {
        destino = nuevo
        authPersonalizada = auth.trim()
        ordenesPersonalizada = ordenes.trim()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("destino", nuevo.name)
            .putString("auth", authPersonalizada)
            .putString("ordenes", ordenesPersonalizada)
            .apply()
        // Cambió el servidor: el token del anterior no sirve
        Sesion.cerrar()
    }

    /** Valida una dirección escrita a mano (debe ser https, salvo la del emulador). */
    fun validarDireccion(url: String): String? = when {
        url.isBlank() -> "Ingresa la dirección"
        !url.trim().startsWith("https://") -> "Debe empezar con https://"
        else -> null
    }

    private fun conBarra(url: String) = if (url.endsWith("/")) url else "$url/"
}
