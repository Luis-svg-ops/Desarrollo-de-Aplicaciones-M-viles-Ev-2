package cl.duoc.acis.ordenes.model;

/** Prioridad de la OT. */
public enum Prioridad {
    ALTA("Alta"),
    MEDIA("Media"),
    BAJA("Baja");

    private final String texto;

    Prioridad(String texto) {
        this.texto = texto;
    }

    public String getTexto() {
        return texto;
    }
}
