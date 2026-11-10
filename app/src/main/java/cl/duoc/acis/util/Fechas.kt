package cl.duoc.acis.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ZONA: ZoneId = ZoneId.systemDefault()
private val HORA = DateTimeFormatter.ofPattern("HH:mm")
private val DIA_MES = DateTimeFormatter.ofPattern("dd-MMM", Locale.forLanguageTag("es-CL"))

private fun fechaDe(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZONA).toLocalDate()

/** True si la fecha (en milisegundos) es hoy. */
fun esHoy(millis: Long): Boolean = fechaDe(millis) == LocalDate.now(ZONA)

/** "Hoy 09:00", "Mañana 10:00", "Ayer 16:00" o "12-nov 09:00". */
fun fechaHoraCorta(millis: Long): String {
    val fecha = fechaDe(millis)
    val hoy = LocalDate.now(ZONA)
    val dia = when (fecha) {
        hoy -> "Hoy"
        hoy.plusDays(1) -> "Mañana"
        hoy.minusDays(1) -> "Ayer"
        else -> fecha.format(DIA_MES)
    }
    val hora = Instant.ofEpochMilli(millis).atZone(ZONA).toLocalTime().format(HORA)
    return "$dia $hora"
}
