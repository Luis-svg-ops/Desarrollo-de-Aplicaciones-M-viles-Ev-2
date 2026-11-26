package cl.duoc.acis.repository

import cl.duoc.acis.data.remote.ClimaApi
import cl.duoc.acis.data.remote.Resultado
import cl.duoc.acis.data.remote.llamarApi
import cl.duoc.acis.util.Clima
import cl.duoc.acis.util.crearClima

/**
 * Clima actual de un local (API externa Open-Meteo). No se guarda en Room: el clima cambia
 * cada hora y no sirve una copia vieja. Si no hay señal, la tarjeta simplemente lo avisa.
 */
class ClimaRepository(private val api: ClimaApi) {

    suspend fun climaEn(latitud: Double, longitud: Double): Resultado<Clima> =
        when (val r = llamarApi { api.actual(latitud, longitud) }) {
            is Resultado.Error -> r
            is Resultado.Exito -> with(r.datos.actual) {
                Resultado.Exito(crearClima(temperatura, codigo, vientoKmh, lluviaMm))
            }
        }
}
