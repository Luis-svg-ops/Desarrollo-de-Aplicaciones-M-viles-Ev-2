package cl.duoc.acis.model

/** Quién usa la app. El cliente (administrador de un local) solo mira el avance de sus OT. */
enum class Rol(val texto: String) {
    TECNICO("Técnico de terreno"),
    JEFE("Jefe de mantenimiento"),
    CLIENTE("Cliente")
}
