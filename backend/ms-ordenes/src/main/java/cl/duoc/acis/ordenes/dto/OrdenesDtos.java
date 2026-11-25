package cl.duoc.acis.ordenes.dto;

import cl.duoc.acis.ordenes.model.AccionOT;
import cl.duoc.acis.ordenes.model.Equipo;
import cl.duoc.acis.ordenes.model.EstadoOT;
import cl.duoc.acis.ordenes.model.EventoOT;
import cl.duoc.acis.ordenes.model.Local;
import cl.duoc.acis.ordenes.model.OrdenTrabajo;
import cl.duoc.acis.ordenes.model.Prioridad;
import cl.duoc.acis.ordenes.model.TipoEquipo;
import cl.duoc.acis.ordenes.model.TipoOT;
import cl.duoc.acis.ordenes.model.UbicacionEquipo;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Forma del JSON que entra y sale de ms-ordenes. Las validaciones son las mismas que en la app
 * (mismos largos y mensajes): la app avisa al tiro y el servidor vuelve a revisar.
 */
public final class OrdenesDtos {

    private OrdenesDtos() {
    }

    private static final String TELEFONO = "^\\+?[0-9 ]{8,16}$";

    // ---------- Entrada ----------

    public record LocalRequest(
            @NotBlank(message = "El código es obligatorio")
            @Pattern(regexp = "^[A-Z0-9-]{4,20}$", message = "Código: 4 a 20 letras mayúsculas, números o guiones") String codigo,
            @NotBlank(message = "El nombre es obligatorio") @Size(max = 60, message = "Máximo 60 caracteres") String nombre,
            @NotBlank(message = "La dirección es obligatoria") @Size(max = 80, message = "Máximo 80 caracteres") String direccion,
            @NotBlank(message = "La comuna es obligatoria") @Size(max = 40, message = "Máximo 40 caracteres") String comuna,
            @NotBlank(message = "El administrador es obligatorio") @Size(max = 60, message = "Máximo 60 caracteres") String nombreAdministrador,
            @NotBlank(message = "El teléfono es obligatorio")
            @Pattern(regexp = TELEFONO, message = "Teléfono no válido (ej.: +56 9 1234 5678)") String telefono,
            @DecimalMin(value = "-90", message = "Latitud no válida") @DecimalMax(value = "90", message = "Latitud no válida") Double latitud,
            @DecimalMin(value = "-180", message = "Longitud no válida") @DecimalMax(value = "180", message = "Longitud no válida") Double longitud) {
    }

    public record EquipoRequest(
            @NotNull(message = "El local es obligatorio") Long localId,
            @NotNull(message = "El tipo es obligatorio") TipoEquipo tipo,
            @NotBlank(message = "La marca es obligatoria") @Size(max = 40, message = "Máximo 40 caracteres") String marca,
            @NotBlank(message = "El modelo es obligatorio") @Size(max = 40, message = "Máximo 40 caracteres") String modelo,
            @NotNull(message = "La ubicación es obligatoria") UbicacionEquipo ubicacion,
            Long fechaUltimaMantencion) {
    }

    /** Crear o editar una OT. El local sale del equipo; el estado lo decide la máquina de estados. */
    public record OtRequest(
            @NotNull(message = "El equipo es obligatorio") Long equipoId,
            Long tecnicoId,
            @NotNull(message = "El tipo es obligatorio") TipoOT tipo,
            @NotNull(message = "La prioridad es obligatoria") Prioridad prioridad,
            @NotBlank(message = "La descripción es obligatoria")
            @Size(min = 10, max = 300, message = "La descripción debe tener entre 10 y 300 caracteres") String descripcion,
            @Size(max = 40, message = "Máximo 40 caracteres") String falla,
            @NotNull(message = "La fecha programada es obligatoria") Long fechaProgramada) {
    }

    public record AccionRequest(@NotNull(message = "La acción es obligatoria") AccionOT accion, String motivo) {
    }

    public record CheckInRequest(
            @NotNull(message = "Falta la latitud")
            @DecimalMin(value = "-90", message = "Latitud no válida") @DecimalMax(value = "90", message = "Latitud no válida") Double latitud,
            @NotNull(message = "Falta la longitud")
            @DecimalMin(value = "-180", message = "Longitud no válida") @DecimalMax(value = "180", message = "Longitud no válida") Double longitud) {
    }

    public record CierreRequest(
            @NotBlank(message = "Ingresa el diagnóstico")
            @Size(min = 10, max = 500, message = "El diagnóstico debe tener entre 10 y 500 caracteres") String diagnostico,
            @NotBlank(message = "Ingresa las actividades realizadas")
            @Size(min = 10, max = 500, message = "Las actividades deben tener entre 10 y 500 caracteres") String actividades,
            @Size(max = 500, message = "Máximo 500 caracteres") String materiales,
            @NotBlank(message = "Toma una foto del equipo") String fotoUri) {
    }

    // ---------- Salida ----------

    public record LocalResponse(Long id, String codigo, String nombre, String direccion, String comuna,
                                String nombreAdministrador, String telefono, Double latitud, Double longitud) {
        public static LocalResponse de(Local l) {
            return new LocalResponse(l.getId(), l.getCodigo(), l.getNombre(), l.getDireccion(), l.getComuna(),
                    l.getNombreAdministrador(), l.getTelefono(), l.getLatitud(), l.getLongitud());
        }
    }

    /** Lo único que se entrega públicamente al buscar un código (lo usa ms-auth en el registro). */
    public record LocalResumen(Long id, String nombre) {
    }

    public record EquipoResponse(Long id, Long localId, TipoEquipo tipo, String marca, String modelo,
                                 UbicacionEquipo ubicacion, Long fechaUltimaMantencion, boolean dadoDeBaja) {
        public static EquipoResponse de(Equipo e) {
            return new EquipoResponse(e.getId(), e.getLocal().getId(), e.getTipo(), e.getMarca(), e.getModelo(),
                    e.getUbicacion(), e.getFechaUltimaMantencion(), e.isDadoDeBaja());
        }
    }

    /** OT con su local, equipo y técnico (igual que OtConDetalle en la app). */
    public record OtResponse(Long id, String numero, EstadoOT estado, TipoOT tipo, Prioridad prioridad,
                             String descripcion, String falla, long fechaCreacion, long fechaProgramada,
                             Long fechaAceptacion, Long checkInFechaHora, Double checkInLat, Double checkInLon,
                             Long checkOutFechaHora, String diagnostico, String actividadesRealizadas,
                             String materialesUtilizados, String fotoUri, String motivo,
                             Long tecnicoId, String tecnicoNombre, LocalResponse local, EquipoResponse equipo) {
        public static OtResponse de(OrdenTrabajo o) {
            return new OtResponse(o.getId(), o.getNumero(), o.getEstado(), o.getTipo(), o.getPrioridad(),
                    o.getDescripcion(), o.getFalla(), o.getFechaCreacion(), o.getFechaProgramada(),
                    o.getFechaAceptacion(), o.getCheckInFechaHora(), o.getCheckInLat(), o.getCheckInLon(),
                    o.getCheckOutFechaHora(), o.getDiagnostico(), o.getActividadesRealizadas(),
                    o.getMaterialesUtilizados(), o.getFotoUri(), o.getMotivo(),
                    o.getTecnicoId(), o.getTecnicoNombre(), LocalResponse.de(o.getLocal()), EquipoResponse.de(o.getEquipo()));
        }
    }

    public record EventoResponse(Long id, EstadoOT estado, String descripcion, long fecha) {
        public static EventoResponse de(EventoOT e) {
            return new EventoResponse(e.getId(), e.getEstado(), e.getDescripcion(), e.getFecha());
        }
    }

    public record MensajeResponse(String mensaje) {
    }
}
