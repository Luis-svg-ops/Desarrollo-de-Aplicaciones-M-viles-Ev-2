package cl.duoc.acis.data.remote

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Token de la sesión actual (el "pase" firmado que entrega ms-auth al iniciar sesión).
 * Vive en memoria: al cerrar la sesión o la app se borra y hay que volver a entrar.
 */
object Sesion {
    @Volatile
    var token: String? = null

    fun cerrar() {
        token = null
    }
}

/**
 * Crea los clientes de Retrofit (igual que RetrofitInstance del ejemplo del profe, pero con 2 servidores).
 * 10.0.2.2 es el "localhost" del computador visto desde el emulador.
 */
object ClienteApi {

    // Nombres "de mentira": el interceptor de abajo los cambia por el servidor elegido en ⚙ Servidor
    const val URL_AUTH = "http://acis-auth/"
    const val URL_ORDENES = "http://acis-ordenes/"

    /** Agrega "Authorization: Bearer <token>" a cada pedido, si hay sesión. */
    private val agregarToken = Interceptor { cadena ->
        val pedido = cadena.request()
        val token = Sesion.token
        cadena.proceed(
            if (token == null) pedido
            else pedido.newBuilder().header("Authorization", "Bearer $token").build()
        )
    }

    /**
     * Manda cada pedido al servidor elegido (nube, emulador u otra dirección) sin tener que recrear Retrofit:
     * cambia el inicio de la URL ("http://acis-auth/") por la dirección real.
     */
    private val elegirServidor = Interceptor { cadena ->
        val pedido = cadena.request()
        val destino = when (pedido.url().host()) {
            "acis-auth" -> ConfigServidor.urlAuth
            "acis-ordenes" -> ConfigServidor.urlOrdenes
            else -> null
        }
        val base = destino?.let { okhttp3.HttpUrl.parse(it) }
        cadena.proceed(
            if (base == null) pedido
            else pedido.newBuilder().url(
                pedido.url().newBuilder().scheme(base.scheme()).host(base.host()).port(base.port()).build()
            ).build()
        )
    }

    /**
     * Tiempos de espera: el servidor gratis "se duerme" y la primera respuesta puede tardar ~50 s,
     * por eso se espera hasta 60 s. Sin internet el error llega al tiro (no espera).
     */
    private val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(elegirServidor)
            .addInterceptor(agregarToken)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private fun retrofit(url: String): Retrofit = Retrofit.Builder()
        .baseUrl(url)
        .client(http)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // "lazy": se crean la primera vez que se usan, una sola vez para toda la app
    val auth: AuthApi by lazy { retrofit(URL_AUTH).create(AuthApi::class.java) }
    val ordenes: OrdenesApi by lazy { retrofit(URL_ORDENES).create(OrdenesApi::class.java) }

    const val URL_CLIMA = "https://api.open-meteo.com/"

    /**
     * API externa: usa su propio cliente HTTP, SIN el interceptor del token.
     * Nuestro token es solo para nuestros microservicios: nunca se le manda a un tercero.
     */
    val clima: ClimaApi by lazy {
        Retrofit.Builder()
            .baseUrl(URL_CLIMA)
            .client(OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS).build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ClimaApi::class.java)
    }
}
