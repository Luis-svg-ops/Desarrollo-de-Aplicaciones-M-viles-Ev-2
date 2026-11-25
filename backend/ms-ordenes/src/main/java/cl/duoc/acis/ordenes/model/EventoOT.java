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

/** Un cambio en la vida de una OT ("Aceptada por Pedro Soto"). Forma el historial. */
@Entity
@Table(name = "eventos_ot")
public class EventoOT {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private OrdenTrabajo ot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoOT estado;

    @Column(nullable = false, length = 200)
    private String descripcion;

    private long fecha;

    protected EventoOT() {
    }

    public EventoOT(OrdenTrabajo ot, EstadoOT estado, String descripcion, long fecha) {
        this.ot = ot;
        this.estado = estado;
        this.descripcion = descripcion;
        this.fecha = fecha;
    }

    public Long getId() { return id; }
    public EstadoOT getEstado() { return estado; }
    public String getDescripcion() { return descripcion; }
    public long getFecha() { return fecha; }
}
