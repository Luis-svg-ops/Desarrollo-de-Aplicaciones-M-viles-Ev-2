package cl.duoc.acis.ordenes.model;

import java.util.List;
import java.util.Set;

import static cl.duoc.acis.ordenes.model.EstadoOT.*;

/**
 * Acciones que cambian el estado de una OT: desde qué estados se pueden hacer, a qué estado llevan,
 * qué rol las hace y qué motivos se pueden elegir (lista vacía = no pide motivo).
 * Es la misma tabla que AccionOT.kt de la app: las reglas viven en los dos lados.
 */
public enum AccionOT {

    ACEPTAR(Set.of(ASIGNADA), ACEPTADA, Rol.TECNICO, List.of()),
    RECHAZAR(Set.of(ASIGNADA), RECHAZADA, Rol.TECNICO,
            List.of("Fuera de mi zona", "Sin herramientas o repuestos", "Superpuesta con otra visita")),
    CHECK_IN(Set.of(ACEPTADA), EN_EJECUCION, Rol.TECNICO, List.of()),
    VISITA_FALLIDA(Set.of(ACEPTADA), EstadoOT.VISITA_FALLIDA, Rol.TECNICO,
            List.of("Local cerrado", "Sin acceso a la azotea", "Encargado ausente")),
    CERRAR(Set.of(EN_EJECUCION), CERRADA, Rol.TECNICO, List.of()),
    CONFORMIDAD(Set.of(CERRADA), CONFORME, Rol.JEFE, List.of()),
    DISCONFORMIDAD(Set.of(CERRADA), EN_EJECUCION, Rol.JEFE,
            List.of("Foto poco clara", "Trabajo incompleto", "Falta información en el diagnóstico")),
    POSTERGAR(Set.of(ASIGNADA, ACEPTADA), POSTERGADA, Rol.JEFE,
            List.of("Lluvia o viento fuerte", "Solicitud del cliente", "Falta de repuesto")),
    CANCELAR(Set.of(CREADA, ASIGNADA, ACEPTADA, RECHAZADA, EstadoOT.VISITA_FALLIDA, POSTERGADA), CANCELADA, Rol.JEFE,
            List.of("El cliente ya no la necesita", "OT duplicada", "Error de registro"));

    private final Set<EstadoOT> desde;
    private final EstadoOT hacia;
    private final Rol rol;
    private final List<String> motivos;

    AccionOT(Set<EstadoOT> desde, EstadoOT hacia, Rol rol, List<String> motivos) {
        this.desde = desde;
        this.hacia = hacia;
        this.rol = rol;
        this.motivos = motivos;
    }

    public Set<EstadoOT> getDesde() { return desde; }
    public EstadoOT getHacia() { return hacia; }
    public Rol getRol() { return rol; }
    public List<String> getMotivos() { return motivos; }

    public boolean pideMotivo() {
        return !motivos.isEmpty();
    }
}
