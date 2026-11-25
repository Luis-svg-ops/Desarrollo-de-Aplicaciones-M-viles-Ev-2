package cl.duoc.acis.ordenes.service;

import cl.duoc.acis.ordenes.dto.OrdenesDtos.CheckInRequest;
import cl.duoc.acis.ordenes.dto.OrdenesDtos.CierreRequest;
import cl.duoc.acis.ordenes.dto.OrdenesDtos.OtRequest;
import cl.duoc.acis.ordenes.exception.AcisException;
import cl.duoc.acis.ordenes.model.AccionOT;
import cl.duoc.acis.ordenes.model.Equipo;
import cl.duoc.acis.ordenes.model.EstadoOT;
import cl.duoc.acis.ordenes.model.EventoOT;
import cl.duoc.acis.ordenes.model.MaquinaEstadosOT;
import cl.duoc.acis.ordenes.model.OrdenTrabajo;
import cl.duoc.acis.ordenes.repository.EventoRepository;
import cl.duoc.acis.ordenes.repository.OrdenTrabajoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Reglas de las OT en el servidor. Cada cambio de estado pasa por la máquina de estados
 * y queda registrado en el historial.
 */
@Service
public class OrdenService {

    /** Margen para no rechazar una OT programada "ahora mismo" por segundos de diferencia. */
    private static final long MARGEN_MS = 60_000;

    private final OrdenTrabajoRepository ordenes;
    private final EventoRepository eventos;
    private final EquipoService equipos;
    private final DirectorioTecnicos tecnicos;
    private final Clock reloj;

    public OrdenService(OrdenTrabajoRepository ordenes, EventoRepository eventos, EquipoService equipos,
                        DirectorioTecnicos tecnicos, Clock reloj) {
        this.ordenes = ordenes;
        this.eventos = eventos;
        this.equipos = equipos;
        this.tecnicos = tecnicos;
        this.reloj = reloj;
    }

    private long ahora() {
        return reloj.millis();
    }

    // ---------- Lectura (filtrada por rol, con los datos del token) ----------

    /** Jefe y admin ven todas; el técnico, las suyas; el cliente, las de su local. */
    public List<OrdenTrabajo> listar(UsuarioActual u) {
        return switch (u.rol()) {
            case JEFE, ADMIN -> ordenes.findAllByOrderByFechaProgramada();
            case TECNICO -> ordenes.findByTecnicoIdOrderByFechaProgramada(u.id());
            case CLIENTE -> u.localId() == null ? List.of() : ordenes.findByLocalIdOrderByFechaProgramada(u.localId());
        };
    }

    /** Si la OT no le corresponde, se responde 404 (ni siquiera se revela que existe). */
    public OrdenTrabajo ver(Long id, UsuarioActual u) {
        OrdenTrabajo ot = ordenes.findById(id).orElseThrow(this::noEncontrada);
        boolean puede = switch (u.rol()) {
            case JEFE, ADMIN -> true;
            case TECNICO -> Objects.equals(ot.getTecnicoId(), u.id());
            case CLIENTE -> Objects.equals(ot.getLocal().getId(), u.localId());
        };
        if (!puede) {
            throw noEncontrada();
        }
        return ot;
    }

    public List<EventoOT> historial(Long id, UsuarioActual u) {
        ver(id, u);
        return eventos.findByOtIdOrderByFechaDescIdDesc(id);
    }

    private AcisException noEncontrada() {
        return new AcisException.NoEncontrado("OT no encontrada");
    }

    // ---------- Crear, editar y eliminar (jefe) ----------

    @Transactional
    public OrdenTrabajo crear(OtRequest p, UsuarioActual jefe) {
        Equipo equipo = equipoActivo(p.equipoId());
        if (p.fechaProgramada() < ahora() - MARGEN_MS) {
            throw new AcisException.DatoInvalido("La fecha programada ya pasó");
        }
        String nombreTecnico = nombreTecnico(p.tecnicoId(), jefe);
        OrdenTrabajo ot = ordenes.save(new OrdenTrabajo(equipo.getLocal(), equipo, p.tecnicoId(), nombreTecnico,
                p.tipo(), p.prioridad(), p.descripcion().trim(), vacioANull(p.falla()), ahora(), p.fechaProgramada()));
        ot.asignarNumero(LocalDate.now(reloj).getYear());
        registrar(ot, EstadoOT.CREADA, "Creada por la jefatura");
        if (nombreTecnico != null) {
            registrar(ot, EstadoOT.ASIGNADA, "Asignada a " + nombreTecnico);
        }
        return ot;
    }

    /** Formulario de edición: asignar, reasignar, quitar técnico o reprogramar. */
    @Transactional
    public OrdenTrabajo editar(Long id, OtRequest p, UsuarioActual jefe) {
        OrdenTrabajo ot = ordenes.findById(id).orElseThrow(this::noEncontrada);
        EstadoOT anterior = ot.getEstado();
        if (!MaquinaEstadosOT.sePuedeEditar(anterior)) {
            throw new AcisException.Conflicto("Una OT " + anterior.getTexto().toLowerCase() + " no se puede editar");
        }
        Equipo equipo = p.equipoId().equals(ot.getEquipo().getId()) ? ot.getEquipo() : equipoActivo(p.equipoId());
        boolean cambiaTecnico = !Objects.equals(p.tecnicoId(), ot.getTecnicoId());
        String nombreTecnico = cambiaTecnico ? nombreTecnico(p.tecnicoId(), jefe) : ot.getTecnicoNombre();
        EstadoOT nuevo = MaquinaEstadosOT.estadoAlEditar(anterior, ot.getTecnicoId(), p.tecnicoId());

        ot.editar(equipo, p.tecnicoId(), nombreTecnico, p.tipo(), p.prioridad(), p.descripcion().trim(),
                vacioANull(p.falla()), p.fechaProgramada(), nuevo);

        String descripcion;
        if (nuevo == EstadoOT.CREADA && anterior != EstadoOT.CREADA) {
            descripcion = "Técnico quitado por la jefatura";
        } else if (nuevo == EstadoOT.ASIGNADA && cambiaTecnico) {
            descripcion = "Asignada a " + nombreTecnico;
        } else if (nuevo != anterior) {
            descripcion = "Reprogramada para " + nombreTecnico;
        } else {
            descripcion = "Datos editados por la jefatura";
        }
        registrar(ot, nuevo, descripcion);
        return ot;
    }

    /** Solo se borra una OT creada por error (sin técnico ni avance). Las demás se cancelan. */
    @Transactional
    public void eliminar(Long id) {
        OrdenTrabajo ot = ordenes.findById(id).orElseThrow(this::noEncontrada);
        if (ot.getEstado() != EstadoOT.CREADA) {
            throw new AcisException.Conflicto("Solo se puede eliminar una OT recién creada. Usa Cancelar para no perder el historial");
        }
        eventos.deleteByOtId(id);
        ordenes.delete(ot);
    }

    // ---------- Acciones (máquina de estados) ----------

    /** Aceptar, rechazar, visita fallida, conformidad, disconformidad, postergar o cancelar. */
    @Transactional
    public OrdenTrabajo aplicar(Long id, AccionOT accion, String motivo, UsuarioActual u) {
        if (accion == AccionOT.CHECK_IN || accion == AccionOT.CERRAR) {
            throw new AcisException.DatoInvalido("El check-in y el cierre tienen su propio endpoint");
        }
        OrdenTrabajo ot = validarAccion(id, accion, motivo, u);
        ot.aplicar(accion, motivo, ahora());
        registrar(ot, accion.getHacia(), describir(accion, u.nombre(), motivo));
        return ot;
    }

    @Transactional
    public OrdenTrabajo checkIn(Long id, CheckInRequest p, UsuarioActual u) {
        OrdenTrabajo ot = validarAccion(id, AccionOT.CHECK_IN, null, u);
        ot.aplicar(AccionOT.CHECK_IN, null, ahora());
        ot.registrarCheckIn(ahora(), p.latitud(), p.longitud());
        registrar(ot, EstadoOT.EN_EJECUCION, "Check-in de " + u.nombre() + " en el local");
        return ot;
    }

    @Transactional
    public OrdenTrabajo cerrar(Long id, CierreRequest p, UsuarioActual u) {
        OrdenTrabajo ot = validarAccion(id, AccionOT.CERRAR, null, u);
        ot.aplicar(AccionOT.CERRAR, null, ahora());
        ot.registrarCierre(ahora(), p.diagnostico().trim(), p.actividades().trim(), vacioANull(p.materiales()), p.fotoUri());
        // La OT cerrada es la mantención más reciente del equipo
        ot.getEquipo().registrarMantencion(ahora());
        registrar(ot, EstadoOT.CERRADA, "Trabajo terminado por " + u.nombre() + " (diagnóstico y foto)");
        return ot;
    }

    /**
     * Busca la OT (respetando lo que el usuario puede ver) y le pregunta a la máquina de estados.
     * Un rol equivocado responde 403; un estado que no permite la acción responde 409.
     */
    private OrdenTrabajo validarAccion(Long id, AccionOT accion, String motivo, UsuarioActual u) {
        OrdenTrabajo ot = ver(id, u);
        MaquinaEstadosOT.validar(accion, ot.getEstado(), u.rol(), motivo, ot.getTecnicoId(), u.id())
                .ifPresent(error -> {
                    if (u.rol() != accion.getRol() || error.startsWith("Esta OT no está asignada")) {
                        throw new AcisException.Prohibido(error);
                    }
                    if (error.startsWith("Acción no permitida")) {
                        throw new AcisException.Conflicto(error);
                    }
                    throw new AcisException.DatoInvalido(error);
                });
        return ot;
    }

    /** Texto del historial (el mismo que usa la app). */
    static String describir(AccionOT accion, String quien, String motivo) {
        String conMotivo = motivo == null ? "" : ": " + motivo;
        return switch (accion) {
            case ACEPTAR -> "Aceptada por " + quien;
            case RECHAZAR -> "Rechazada por " + quien + conMotivo;
            case CHECK_IN -> "Check-in de " + quien + " en el local";
            case VISITA_FALLIDA -> "Visita fallida" + conMotivo;
            case CERRAR -> "Trabajo terminado por " + quien + " (diagnóstico y foto)";
            case CONFORMIDAD -> "Conformidad de la jefatura";
            case DISCONFORMIDAD -> "Disconformidad de la jefatura" + conMotivo;
            case POSTERGAR -> "Postergada" + conMotivo;
            case CANCELAR -> "Cancelada" + conMotivo;
        };
    }

    // ---------- Ayudas ----------

    private Equipo equipoActivo(Long equipoId) {
        Equipo equipo = equipos.buscar(equipoId);
        if (equipo.isDadoDeBaja()) {
            throw new AcisException.DatoInvalido("El equipo está dado de baja");
        }
        return equipo;
    }

    /** El nombre del técnico se consulta a ms-auth: así no se puede asignar un técnico inventado o desactivado. */
    private String nombreTecnico(Long tecnicoId, UsuarioActual jefe) {
        if (tecnicoId == null) {
            return null;
        }
        return tecnicos.nombreDe(tecnicoId, jefe.token())
                .orElseThrow(() -> new AcisException.DatoInvalido("El técnico no existe o está desactivado"));
    }

    private void registrar(OrdenTrabajo ot, EstadoOT estado, String descripcion) {
        eventos.save(new EventoOT(ot, estado, descripcion, ahora()));
    }

    private static String vacioANull(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
