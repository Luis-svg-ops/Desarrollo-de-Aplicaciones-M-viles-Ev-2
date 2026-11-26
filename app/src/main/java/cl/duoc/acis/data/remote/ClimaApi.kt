package cl.duoc.acis.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * API EXTERNA y pública: Open-Meteo (https://open-meteo.com). Es gratis y no pide clave.
 * Se diferencia de los microservicios propios en que no es nuestra: no le mandamos el token,
 * solo las coordenadas del local, y su respuesta no se guarda en Room (el clima cambia).
 *
 * Ejemplo: GET https://api.open-meteo.com/v1/forecast?latitude=-33.49&longitude=-70.71
 *          &current=temperature_2m,precipitation,weather_code,wind_speed_10m&timezone=auto
 */
interface ClimaApi {

    @GET("v1/forecast")
    suspend fun actual(
        @Query("latitude") latitud: Double,
        @Query("longitude") longitud: Double,
        @Query("current") datos: String = "temperature_2m,precipitation,weather_code,wind_speed_10m",
        @Query("timezone") zona: String = "auto"
    ): ClimaResponse
}

/** Solo se lee la parte "current" del JSON. */
data class ClimaResponse(@SerializedName("current") val actual: ClimaActualDto)

/** @SerializedName conecta el nombre del JSON (temperature_2m) con el nombre en Kotlin. */
data class ClimaActualDto(
    @SerializedName("time") val hora: String,
    @SerializedName("temperature_2m") val temperatura: Double,
    @SerializedName("precipitation") val lluviaMm: Double,
    @SerializedName("weather_code") val codigo: Int,
    @SerializedName("wind_speed_10m") val vientoKmh: Double
)
