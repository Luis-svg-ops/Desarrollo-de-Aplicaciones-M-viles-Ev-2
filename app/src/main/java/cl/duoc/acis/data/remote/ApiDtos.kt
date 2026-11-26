package cl.duoc.acis.data.remote

import cl.duoc.acis.model.EstadoOT
import cl.duoc.acis.model.Prioridad
import cl.duoc.acis.model.Rol
import cl.duoc.acis.model.TipoEquipo
import cl.duoc.acis.model.TipoOT
import cl.duoc.acis.model.UbicacionEquipo

/*
 * DTO = forma del JSON que viaja entre la app y los microservicios.
 * Gson convierte el JSON a estas clases (y al revés) usando los nombres de los campos,
 * que son los mismos del backend. Se separan de las entidades de Room a propósito:
 * si el servidor cambia un campo, solo se toca aquí y en el traductor (Traductor.kt).
 */

// ---------- ms-auth ----------

data class LoginRequest(val correo: String, val clave: String)

data class UsuarioDto(
    val id: Long,
    val nombre: String,
    val correo: String,
    val telefono: String?,
    val rol: Rol,
    val estado: String,
    val localId: Long?
)

data class LoginResponse(val token: String, val tipo: String, val expiraEnSegundos: Long, val usuario: UsuarioDto)

// ---------- ms-ordenes ----------

data class LocalDto(
    val id: Long,
    val codigo: String,
    val nombre: String,
    val direccion: String,
    val comuna: String,
    val nombreAdministrador: String,
    val telefono: String,
    val latitud: Double?,
    val longitud: Double?
)

data class EquipoDto(
    val id: Long,
    val localId: Long,
    val tipo: TipoEquipo,
    val marca: String,
    val modelo: String,
    val ubicacion: UbicacionEquipo,
    val fechaUltimaMantencion: Long?,
    val dadoDeBaja: Boolean
)

data class OtDto(
    val id: Long,
    val numero: String,
    val estado: EstadoOT,
    val tipo: TipoOT,
    val prioridad: Prioridad,
    val descripcion: String,
    val falla: String?,
    val fechaCreacion: Long,
    val fechaProgramada: Long,
    val fechaAceptacion: Long?,
    val checkInFechaHora: Long?,
    val checkInLat: Double?,
    val checkInLon: Double?,
    val checkOutFechaHora: Long?,
    val diagnostico: String?,
    val actividadesRealizadas: String?,
    val materialesUtilizados: String?,
    val fotoUri: String?,
    val motivo: String?,
    val tecnicoId: Long?,
    val tecnicoNombre: String?,
    val local: LocalDto,
    val equipo: EquipoDto
)

data class EventoDto(val id: Long, val estado: EstadoOT, val descripcion: String, val fecha: Long)

data class OtRequest(
    val equipoId: Long,
    val tecnicoId: Long?,
    val tipo: TipoOT,
    val prioridad: Prioridad,
    val descripcion: String,
    val falla: String?,
    val fechaProgramada: Long
)

data class AccionRequest(val accion: String, val motivo: String?)

data class CheckInRequest(val latitud: Double, val longitud: Double)

data class CierreRequest(val diagnostico: String, val actividades: String, val materiales: String?, val fotoUri: String)

data class EquipoRequest(
    val localId: Long,
    val tipo: TipoEquipo,
    val marca: String,
    val modelo: String,
    val ubicacion: UbicacionEquipo,
    val fechaUltimaMantencion: Long?
)

// ---------- Comunes ----------

/** Cuerpo de los errores del backend: {"status": 409, "error": "..."} */
data class ErrorDto(val status: Int?, val error: String?)

data class MensajeDto(val mensaje: String)
