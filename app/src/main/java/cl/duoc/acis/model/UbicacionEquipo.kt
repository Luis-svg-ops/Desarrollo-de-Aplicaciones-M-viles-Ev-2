package cl.duoc.acis.model

/** Los equipos exteriores (azoteas, patios) son los que se ven afectados por el clima. */
enum class UbicacionEquipo(val texto: String) {
    INTERIOR("Interior"),
    EXTERIOR("Exterior / azotea")
}
