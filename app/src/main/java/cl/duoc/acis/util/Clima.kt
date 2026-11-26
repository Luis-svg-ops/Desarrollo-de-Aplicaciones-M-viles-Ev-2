package cl.duoc.acis.util

import kotlin.math.roundToInt

/** Clima actual en el local, ya traducido para mostrarlo. */
data class Clima(
    val temperatura: Int,
    val descripcion: String,
    val vientoKmh: Int,
    val lluviaMm: Double,
    val codigo: Int
)

/** Viento desde el cual no es seguro trabajar en altura (azoteas). */
const val VIENTO_PELIGROSO_KMH = 40

/**
 * Traduce el código WMO que entrega Open-Meteo a un texto en español
 * (0 despejado, 1-3 nubes, 45-48 niebla, 51-67 llovizna/lluvia, 71-77 nieve, 80-82 chubascos, 95-99 tormenta).
 */
fun descripcionClima(codigo: Int): String = when (codigo) {
    0 -> "Despejado"
    1, 2 -> "Parcialmente nublado"
    3 -> "Nublado"
    45, 48 -> "Niebla"
    in 51..57 -> "Llovizna"
    in 61..67 -> "Lluvia"
    in 71..77 -> "Nieve"
    in 80..82 -> "Chubascos"
    in 95..99 -> "Tormenta"
    else -> "Sin datos"
}

fun esLluvioso(codigo: Int, lluviaMm: Double): Boolean =
    lluviaMm > 0.0 || codigo in 51..67 || codigo in 80..82 || codigo in 95..99

/**
 * Recomendación para el trabajo de hoy. Solo importa si el equipo está afuera (azotea):
 * con lluvia o viento fuerte conviene postergar (es uno de los motivos de postergación de la app).
 * Devuelve null si no hay nada que advertir.
 */
fun recomendacionClima(clima: Clima, equipoExterior: Boolean): String? {
    if (!equipoExterior) return null
    val lluvia = esLluvioso(clima.codigo, clima.lluviaMm)
    val viento = clima.vientoKmh >= VIENTO_PELIGROSO_KMH
    return when {
        lluvia && viento -> "Lluvia y viento fuerte en el local: considera postergar el trabajo en la azotea"
        lluvia -> "Está lloviendo en el local: considera postergar el trabajo en la azotea"
        viento -> "Viento fuerte (${clima.vientoKmh} km/h): trabajar en altura no es seguro"
        else -> null
    }
}

fun crearClima(temperatura: Double, codigo: Int, vientoKmh: Double, lluviaMm: Double) = Clima(
    temperatura = temperatura.roundToInt(),
    descripcion = descripcionClima(codigo),
    vientoKmh = vientoKmh.roundToInt(),
    lluviaMm = lluviaMm,
    codigo = codigo
)
