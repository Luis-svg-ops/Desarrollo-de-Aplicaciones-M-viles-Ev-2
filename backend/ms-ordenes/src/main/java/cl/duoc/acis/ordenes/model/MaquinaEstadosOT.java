package cl.duoc.acis.ordenes.model;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Máquina de estados de la OT en el servidor: la misma que MaquinaEstadosOT.kt de la app.
 * La app la usa para mostrar solo los botones permitidos; el servidor la usa para que nadie
 * se salte las reglas llamando a la API directo ("nunca confíes en el cliente").
 */
public final class MaquinaEstadosOT {

    public static final Set<EstadoOT> FINALES = Set.of(EstadoOT.CONFORME, EstadoOT.CANCELADA);
    public static final String MOTIVO_OTRO = "Otro";
    public static final int MAX_MOTIVO_OTRO = 80;

    private MaquinaEstadosOT() {
    }

    /** Mensaje de error, o vacío si la acción está permitida. */
    public static Optional<String> validar(AccionOT accion, EstadoOT estado, Rol rol, String motivo,
                                           Long tecnicoDeLaOt, Long usuarioId) {
        if (rol != accion.getRol()) {
            return Optional.of("Tu rol no permite esta acción");
        }
        if (rol == Rol.TECNICO && !Objects.equals(tecnicoDeLaOt, usuarioId)) {
            return Optional.of("Esta OT no está asignada a ti");
        }
        if (!accion.getDesde().contains(estado)) {
            return Optional.of("Acción no permitida: la OT está " + estado.getTexto().toLowerCase());
        }
        if (accion.pideMotivo() && (motivo == null || motivo.isBlank())) {
            return Optional.of("Selecciona un motivo");
        }
        if (accion.pideMotivo() && !accion.getMotivos().contains(motivo) && !esMotivoOtro(motivo)) {
            return Optional.of("Motivo no válido");
        }
        return Optional.empty();
    }

    /** Motivo escrito a mano: "Otro: texto" con 1 a 80 caracteres. */
    public static boolean esMotivoOtro(String motivo) {
        if (motivo == null || !motivo.startsWith(MOTIVO_OTRO + ": ")) {
            return false;
        }
        String texto = motivo.substring(MOTIVO_OTRO.length() + 2);
        return !texto.isBlank() && texto.length() <= MAX_MOTIVO_OTRO;
    }

    public static EstadoOT estadoInicial(Long tecnicoId) {
        return tecnicoId == null ? EstadoOT.CREADA : EstadoOT.ASIGNADA;
    }

    /** Una OT en ejecución, cerrada o terminada ya no se edita desde el formulario. */
    public static boolean sePuedeEditar(EstadoOT estado) {
        return estado != EstadoOT.EN_EJECUCION && estado != EstadoOT.CERRADA && !FINALES.contains(estado);
    }

    /** Qué estado queda al guardar el formulario: sin técnico = Creada; técnico nuevo o reprogramada = Asignada. */
    public static EstadoOT estadoAlEditar(EstadoOT estado, Long tecnicoAnterior, Long tecnicoNuevo) {
        if (tecnicoNuevo == null) {
            return EstadoOT.CREADA;
        }
        boolean cambioTecnico = !tecnicoNuevo.equals(tecnicoAnterior);
        boolean detenida = estado != EstadoOT.ASIGNADA && estado != EstadoOT.ACEPTADA;
        return cambioTecnico || detenida ? EstadoOT.ASIGNADA : estado;
    }
}
