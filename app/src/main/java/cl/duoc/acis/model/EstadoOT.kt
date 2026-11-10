package cl.duoc.acis.model

/** Estados por los que pasa una orden de trabajo (Excel: hoja "Estados OT"). */
enum class EstadoOT(val texto: String) {
    CREADA("Creada"),
    ASIGNADA("Asignada"),
    ACEPTADA("Aceptada"),
    RECHAZADA("Rechazada"),
    EN_EJECUCION("En ejecución"),
    CERRADA("Cerrada"),
    POSTERGADA("Postergada"),
    VISITA_FALLIDA("Visita fallida"),
    CANCELADA("Cancelada")
}
