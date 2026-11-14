package cl.duoc.acis.util

import android.content.Context
import android.net.Uri
import cl.duoc.acis.model.EventoOT
import cl.duoc.acis.model.OtConDetalle
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/*
 * Informe de una OT en formato JSON (HU-26), usando Gson: la librería que convierte
 * objetos a JSON y de vuelta (la misma que se usa para hablar con un servidor).
 *
 * Se arma con clases propias (y no con las entidades de Room) para que el archivo sea
 * fácil de leer: textos en vez de ids, y fechas como "2026-11-14T16:30" en vez de milisegundos.
 */

data class InformeOt(
    val numero: String,
    val estado: String,
    val tipo: String,
    val prioridad: String,
    val local: LocalInforme,
    val equipo: EquipoInforme,
    val tecnico: String?,
    val programada: String,
    val checkIn: String?,
    val checkOut: String?,
    val duracion: String?,
    val ubicacionCheckIn: UbicacionInforme?,
    val diagnostico: String?,
    val actividades: String?,
    val materiales: String?,
    val motivo: String?,
    val tieneFotoDeEvidencia: Boolean,
    val historial: List<EventoInforme>,
    val generadoEl: String
)

data class LocalInforme(val nombre: String, val direccion: String, val comuna: String, val contacto: String, val telefono: String)
data class EquipoInforme(val tipo: String, val marca: String, val modelo: String, val ubicacion: String)
data class UbicacionInforme(val latitud: Double, val longitud: Double)
data class EventoInforme(val fecha: String, val estado: String, val descripcion: String)

/** Fecha y hora legibles, sin segundos: "2026-11-14T16:30". */
private fun fechaIso(milisegundos: Long): String =
    Instant.ofEpochMilli(milisegundos).atZone(ZoneId.systemDefault()).toLocalDateTime()
        .truncatedTo(ChronoUnit.MINUTES).toString()

/** Arma el informe con los datos de la OT y su historial (función pura: fácil de probar). */
fun crearInforme(d: OtConDetalle, historial: List<EventoOT>, ahora: Long = System.currentTimeMillis()): InformeOt {
    val ot = d.ot
    val inicio = ot.checkInFechaHora
    val fin = ot.checkOutFechaHora
    val lat = ot.checkInLat
    val lon = ot.checkInLon
    return InformeOt(
        numero = ot.numero,
        estado = ot.estado.texto,
        tipo = ot.tipo.texto,
        prioridad = ot.prioridad.texto,
        local = LocalInforme(d.local.nombre, d.local.direccion, d.local.comuna, d.local.nombreAdministrador, d.local.telefono),
        equipo = EquipoInforme(d.equipo.tipo.texto, d.equipo.marca, d.equipo.modelo, d.equipo.ubicacion.texto),
        tecnico = d.tecnico?.nombre,
        programada = fechaIso(ot.fechaProgramada),
        checkIn = inicio?.let(::fechaIso),
        checkOut = fin?.let(::fechaIso),
        duracion = if (inicio != null && fin != null) formatoDuracion(fin - inicio) else null,
        ubicacionCheckIn = if (lat != null && lon != null) UbicacionInforme(lat, lon) else null,
        diagnostico = ot.diagnostico,
        actividades = ot.actividadesRealizadas,
        materiales = ot.materialesUtilizados,
        motivo = ot.motivo,
        tieneFotoDeEvidencia = ot.fotoUri != null,
        historial = historial.map { EventoInforme(fechaIso(it.fecha), it.estado.texto, it.descripcion) },
        generadoEl = fechaIso(ahora)
    )
}

/** Conversión con Gson. Los datos vacíos (null) no se escriben, así el archivo queda limpio. */
object InformeJson {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun aJson(informe: InformeOt): String = gson.toJson(informe)

    fun desdeJson(json: String): InformeOt = gson.fromJson(json, InformeOt::class.java)
}

/** Nombre sugerido del archivo: "informe_OT-2026-0102.json". */
fun nombreArchivoInforme(numero: String): String = "informe_$numero.json"

/**
 * Escribe el texto en el archivo que eligió el usuario. Escribir en disco es lento:
 * se hace en Dispatchers.IO (otro hilo) para no trabar la pantalla.
 */
suspend fun guardarTexto(context: Context, destino: Uri, texto: String): Boolean = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openOutputStream(destino)?.use { it.write(texto.toByteArray(Charsets.UTF_8)) } != null
    } catch (e: Exception) {
        false
    }
}
