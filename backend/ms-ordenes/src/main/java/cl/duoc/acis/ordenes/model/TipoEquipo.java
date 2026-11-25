package cl.duoc.acis.ordenes.model;

/** Tipo de equipo. */
public enum TipoEquipo {
    CLIMATIZACION("Climatización"),
    REFRIGERACION("Refrigeración");

    private final String texto;

    TipoEquipo(String texto) {
        this.texto = texto;
    }

    public String getTexto() {
        return texto;
    }
}
