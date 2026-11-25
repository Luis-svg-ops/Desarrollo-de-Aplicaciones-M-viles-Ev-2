package cl.duoc.acis.ordenes.model;

/** Estados de una OT (los mismos que la app). */
public enum EstadoOT {
    CREADA("Creada"),
    ASIGNADA("Asignada"),
    ACEPTADA("Aceptada"),
    RECHAZADA("Rechazada"),
    EN_EJECUCION("En ejecución"),
    CERRADA("Cerrada"),
    CONFORME("Conforme"),
    POSTERGADA("Postergada"),
    VISITA_FALLIDA("Visita fallida"),
    CANCELADA("Cancelada");

    private final String texto;

    EstadoOT(String texto) {
        this.texto = texto;
    }

    public String getTexto() {
        return texto;
    }
}
