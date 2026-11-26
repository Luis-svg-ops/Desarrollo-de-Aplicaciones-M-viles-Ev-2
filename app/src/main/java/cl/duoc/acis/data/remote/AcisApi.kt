package cl.duoc.acis.data.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Endpoints de ms-auth (puerto 8090). Retrofit crea la implementación a partir de las anotaciones:
 * @POST("auth/login") significa "manda un POST a /auth/login con este cuerpo en JSON".
 * Todas son "suspend": se llaman desde una corrutina y no traban la pantalla.
 */
interface AuthApi {

    @POST("auth/login")
    suspend fun login(@Body pedido: LoginRequest): LoginResponse

    @GET("usuarios/tecnicos")
    suspend fun tecnicos(): List<UsuarioDto>
}

/** Endpoints de ms-ordenes (puerto 8091). El token se agrega solo (ver ClienteApi). */
interface OrdenesApi {

    @GET("ordenes")
    suspend fun ordenes(): List<OtDto>

    @GET("ordenes/{id}/historial")
    suspend fun historial(@Path("id") id: Long): List<EventoDto>

    @POST("ordenes")
    suspend fun crear(@Body pedido: OtRequest): OtDto

    @PUT("ordenes/{id}")
    suspend fun editar(@Path("id") id: Long, @Body pedido: OtRequest): OtDto

    @POST("ordenes/{id}/acciones")
    suspend fun accion(@Path("id") id: Long, @Body pedido: AccionRequest): OtDto

    @POST("ordenes/{id}/check-in")
    suspend fun checkIn(@Path("id") id: Long, @Body pedido: CheckInRequest): OtDto

    @POST("ordenes/{id}/cierre")
    suspend fun cierre(@Path("id") id: Long, @Body pedido: CierreRequest): OtDto

    @GET("locales")
    suspend fun locales(): List<LocalDto>

    @GET("locales/{id}/equipos")
    suspend fun equipos(@Path("id") localId: Long): List<EquipoDto>

    @POST("equipos")
    suspend fun crearEquipo(@Body pedido: EquipoRequest): EquipoDto

    @PUT("equipos/{id}")
    suspend fun editarEquipo(@Path("id") id: Long, @Body pedido: EquipoRequest): EquipoDto

    @DELETE("equipos/{id}")
    suspend fun eliminarEquipo(@Path("id") id: Long): MensajeDto
}
