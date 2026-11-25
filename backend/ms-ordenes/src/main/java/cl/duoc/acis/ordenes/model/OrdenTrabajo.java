package cl.duoc.acis.ordenes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Orden de trabajo. Los campos son los mismos que en la app (fechas en milisegundos).
 * El técnico vive en ms-auth: aquí se guarda su id y una copia de su nombre para mostrarlo
 * sin tener que preguntarle a ms-auth en cada lista.
 * El estado solo cambia con métodos de esta clase, y el servicio los llama después de validar
 * con la máquina de estados.
 */
@Entity
@Table(name = "ordenes_trabajo")
public class OrdenTrabajo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 20)
    private String numero;

    @ManyToOne(optional = false)
    private Local local;

    @ManyToOne(optional = false)
    private Equipo equipo;

    private Long tecnicoId;

    @Column(length = 60)
    private String tecnicoNombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoOT tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Prioridad prioridad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoOT estado;

    @Column(nullable = false, length = 300)
    private String descripcion;

    @Column(length = 40)
    private String falla;

    private long fechaCreacion;
    private long fechaProgramada;
    private Long fechaAceptacion;
    private Long checkInFechaHora;
    private Double checkInLat;
    private Double checkInLon;
    private Long checkOutFechaHora;

    @Column(length = 500)
    private String diagnostico;

    @Column(length = 500)
    private String actividadesRealizadas;

    @Column(length = 500)
    private String materialesUtilizados;

    /** Referencia a la foto de evidencia que tomó el técnico. */
    @Column(length = 300)
    private String fotoUri;

    @Column(length = 100)
    private String motivo;

    protected OrdenTrabajo() {
    }

    public OrdenTrabajo(Local local, Equipo equipo, Long tecnicoId, String tecnicoNombre, TipoOT tipo,
                        Prioridad prioridad, String descripcion, String falla, long fechaCreacion, long fechaProgramada) {
        this.local = local;
        this.equipo = equipo;
        this.tecnicoId = tecnicoId;
        this.tecnicoNombre = tecnicoNombre;
        this.tipo = tipo;
        this.prioridad = prioridad;
        this.descripcion = descripcion;
        this.falla = falla;
        this.fechaCreacion = fechaCreacion;
        this.fechaProgramada = fechaProgramada;
        this.estado = MaquinaEstadosOT.estadoInicial(tecnicoId);
    }

    /** El número correlativo se calcula con el id que entrega la base (OT-2026-0108). */
    public void asignarNumero(int anio) {
        this.numero = String.format("OT-%d-%04d", anio, 100 + id);
    }

    /** Cambio de estado por una acción ya validada. */
    public void aplicar(AccionOT accion, String motivoNuevo, long ahora) {
        this.estado = accion.getHacia();
        if (accion.pideMotivo()) {
            this.motivo = motivoNuevo;
        }
        if (accion == AccionOT.ACEPTAR) {
            this.fechaAceptacion = ahora;
        }
    }

    public void registrarCheckIn(long ahora, double lat, double lon) {
        this.checkInFechaHora = ahora;
        this.checkInLat = lat;
        this.checkInLon = lon;
    }

    public void registrarCierre(long ahora, String diagnostico, String actividades, String materiales, String fotoUri) {
        this.checkOutFechaHora = ahora;
        this.diagnostico = diagnostico;
        this.actividadesRealizadas = actividades;
        this.materialesUtilizados = materiales;
        this.fotoUri = fotoUri;
        // Si venía de una disconformidad, el motivo ya se corrigió
        this.motivo = null;
    }

    /** Edición del jefe (formulario). El nuevo estado lo calcula la máquina de estados. */
    public void editar(Equipo equipo, Long tecnicoId, String tecnicoNombre, TipoOT tipo, Prioridad prioridad,
                       String descripcion, String falla, long fechaProgramada, EstadoOT nuevoEstado) {
        boolean cambiaEstado = nuevoEstado != this.estado;
        this.equipo = equipo;
        this.local = equipo.getLocal();
        this.tecnicoId = tecnicoId;
        this.tecnicoNombre = tecnicoNombre;
        this.tipo = tipo;
        this.prioridad = prioridad;
        this.descripcion = descripcion;
        this.falla = falla;
        this.fechaProgramada = fechaProgramada;
        if (cambiaEstado) {
            // Reasignada o reprogramada: el motivo anterior ya no aplica y hay que volver a aceptarla
            this.motivo = null;
            this.fechaAceptacion = null;
        }
        this.estado = nuevoEstado;
    }

    /** Solo para los datos de ejemplo: deja la OT en un estado avanzado con sus datos. */
    public void cargarEjemplo(EstadoOT estado, String numero, Long fechaAceptacion, Long checkIn, Long checkOut,
                              String diagnostico, String actividades, String materiales, String motivo) {
        this.estado = estado;
        this.numero = numero;
        this.fechaAceptacion = fechaAceptacion;
        this.checkInFechaHora = checkIn;
        this.checkOutFechaHora = checkOut;
        this.diagnostico = diagnostico;
        this.actividadesRealizadas = actividades;
        this.materialesUtilizados = materiales;
        this.motivo = motivo;
    }

    public Long getId() { return id; }
    public String getNumero() { return numero; }
    public Local getLocal() { return local; }
    public Equipo getEquipo() { return equipo; }
    public Long getTecnicoId() { return tecnicoId; }
    public String getTecnicoNombre() { return tecnicoNombre; }
    public TipoOT getTipo() { return tipo; }
    public Prioridad getPrioridad() { return prioridad; }
    public EstadoOT getEstado() { return estado; }
    public String getDescripcion() { return descripcion; }
    public String getFalla() { return falla; }
    public long getFechaCreacion() { return fechaCreacion; }
    public long getFechaProgramada() { return fechaProgramada; }
    public Long getFechaAceptacion() { return fechaAceptacion; }
    public Long getCheckInFechaHora() { return checkInFechaHora; }
    public Double getCheckInLat() { return checkInLat; }
    public Double getCheckInLon() { return checkInLon; }
    public Long getCheckOutFechaHora() { return checkOutFechaHora; }
    public String getDiagnostico() { return diagnostico; }
    public String getActividadesRealizadas() { return actividadesRealizadas; }
    public String getMaterialesUtilizados() { return materialesUtilizados; }
    public String getFotoUri() { return fotoUri; }
    public String getMotivo() { return motivo; }
}
