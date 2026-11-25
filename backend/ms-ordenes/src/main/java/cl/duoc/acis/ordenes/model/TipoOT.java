package cl.duoc.acis.ordenes.model;

/** Tipo de OT. */
public enum TipoOT {
    PREVENTIVA("Preventiva"),
    CORRECTIVA("Correctiva");

    private final String texto;

    TipoOT(String texto) {
        this.texto = texto;
    }

    public String getTexto() {
        return texto;
    }
}
