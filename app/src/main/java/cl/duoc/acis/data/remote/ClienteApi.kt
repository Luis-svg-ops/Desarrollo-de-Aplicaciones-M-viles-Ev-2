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

    const val URL_AUTH = "http://10.0.2.2:8090/"
    const val URL_ORDENES = "http://10.0.2.2:8091/"

    /** Agrega "Authorization: Bearer <token>" a cada pedido, si hay sesión. */
    private val agregarToken = Interceptor { cadena ->
        val pedido = cadena.request()
        val token = Sesion.token
        cadena.proceed(
            if (token == null) pedido
            else pedido.newBuilder().header("Authorization", "Bearer $token").build()
        )
    }

    /** Si el servidor no responde en 10 segundos se avisa "sin conexión" en vez de esperar para siempre. */
    private val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(agregarToken)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
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
}
