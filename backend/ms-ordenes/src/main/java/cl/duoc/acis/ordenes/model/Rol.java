package cl.duoc.acis.ordenes.model;

/** Roles de ACIS (vienen dentro del token que entrega ms-auth). */
public enum Rol {
    ADMIN("Administrador"),
    JEFE("Jefe de mantenimiento"),
    TECNICO("Técnico de terreno"),
    CLIENTE("Cliente");

    private final String texto;

    Rol(String texto) {
        this.texto = texto;
    }

    public String getTexto() {
        return texto;
    }
}
