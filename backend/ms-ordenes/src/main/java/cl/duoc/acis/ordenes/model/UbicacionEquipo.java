package cl.duoc.acis.ordenes.model;

/** Dónde está instalado el equipo. */
public enum UbicacionEquipo {
    INTERIOR("Interior"),
    EXTERIOR("Exterior / azotea");

    private final String texto;

    UbicacionEquipo(String texto) {
        this.texto = texto;
    }

    public String getTexto() {
        return texto;
    }
}
