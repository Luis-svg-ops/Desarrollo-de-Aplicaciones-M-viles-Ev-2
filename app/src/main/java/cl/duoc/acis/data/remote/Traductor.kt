package cl.duoc.acis.data.remote

import cl.duoc.acis.model.Equipo
import cl.duoc.acis.model.EventoOT
import cl.duoc.acis.model.Local
import cl.duoc.acis.model.OrdenTrabajo
import cl.duoc.acis.model.Rol
import cl.duoc.acis.model.Usuario

/*
 * Traductores: convierten lo que llega del servidor (DTO) en las entidades de Room, y al revés.
 * Así Room guarda una copia de lo que entregó el servidor, con los mismos ids.
 */

fun LocalDto.aEntidad() = Local(
    id = id, nombre = nombre, direccion = direccion, comuna = comuna,
    nombreAdministrador = nombreAdministrador, telefono = telefono,
    codigo = codigo, latitud = latitud, longitud = longitud
)

fun EquipoDto.aEntidad() = Equipo(
    id = id, localId = localId, tipo = tipo, marca = marca, modelo = modelo,
    ubicacion = ubicacion, fechaUltimaMantencion = fechaUltimaMantencion, dadoDeBaja = dadoDeBaja
)

fun OtDto.aEntidad() = OrdenTrabajo(
    id = id, numero = numero, localId = local.id, equipoId = equipo.id, tecnicoId = tecnicoId,
    tipo = tipo, prioridad = prioridad, estado = estado, descripcion = descripcion,
    fechaCreacion = fechaCreacion, fechaProgramada = fechaProgramada, fechaAceptacion = fechaAceptacion,
    checkInFechaHora = checkInFechaHora, checkInLat = checkInLat, checkInLon = checkInLon,
    checkOutFechaHora = checkOutFechaHora, diagnostico = diagnostico,
    actividadesRealizadas = actividadesRealizadas, materialesUtilizados = materialesUtilizados,
    fotoUri = fotoUri, motivo = motivo, falla = falla
)

fun EventoDto.aEntidad(otId: Long) = EventoOT(id = id, otId = otId, estado = estado, descripcion = descripcion, fecha = fecha)

/** La contraseña ya no se guarda en el teléfono: la revisa ms-auth. */
fun UsuarioDto.aEntidad() = Usuario(id = id, nombre = nombre, email = correo, password = "", rol = rol, localId = localId)

/**
 * Técnico que aparece dentro de una OT: solo se conoce su id y nombre. Se guarda para que
 * la OT pueda mostrar "Pedro Soto" (la tabla de OT apunta a la de usuarios).
 */
fun tecnicoDeOt(id: Long, nombre: String?) =
    Usuario(id = id, nombre = nombre ?: "Técnico", email = "usuario$id@acis.local", password = "", rol = Rol.TECNICO)

fun OrdenTrabajo.aPedido() = OtRequest(
    equipoId = equipoId, tecnicoId = tecnicoId, tipo = tipo, prioridad = prioridad,
    descripcion = descripcion, falla = falla, fechaProgramada = fechaProgramada
)

fun Equipo.aPedido() = EquipoRequest(
    localId = localId, tipo = tipo, marca = marca, modelo = modelo,
    ubicacion = ubicacion, fechaUltimaMantencion = fechaUltimaMantencion
)
