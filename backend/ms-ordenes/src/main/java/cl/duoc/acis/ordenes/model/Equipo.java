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

/** Equipo de climatización o refrigeración instalado en un local. */
@Entity
@Table(name = "equipos")
public class Equipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Local local;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoEquipo tipo;

    @Column(nullable = false, length = 40)
    private String marca;

    @Column(nullable = false, length = 40)
    private String modelo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UbicacionEquipo ubicacion;

    /** Milisegundos (igual que la app). */
    private Long fechaUltimaMantencion;

    /** Un equipo con historial no se borra: se da de baja. */
    private boolean dadoDeBaja;

    protected Equipo() {
    }

    public Equipo(Local local, TipoEquipo tipo, String marca, String modelo, UbicacionEquipo ubicacion,
                  Long fechaUltimaMantencion) {
        this.local = local;
        actualizar(tipo, marca, modelo, ubicacion, fechaUltimaMantencion);
    }

    public final void actualizar(TipoEquipo tipo, String marca, String modelo, UbicacionEquipo ubicacion,
                                 Long fechaUltimaMantencion) {
        this.tipo = tipo;
        this.marca = marca;
        this.modelo = modelo;
        this.ubicacion = ubicacion;
        this.fechaUltimaMantencion = fechaUltimaMantencion;
    }

    public void registrarMantencion(long fecha) {
        this.fechaUltimaMantencion = fecha;
    }

    public void darDeBaja() {
        this.dadoDeBaja = true;
    }

    public Long getId() { return id; }
    public Local getLocal() { return local; }
    public TipoEquipo getTipo() { return tipo; }
    public String getMarca() { return marca; }
    public String getModelo() { return modelo; }
    public UbicacionEquipo getUbicacion() { return ubicacion; }
    public Long getFechaUltimaMantencion() { return fechaUltimaMantencion; }
    public boolean isDadoDeBaja() { return dadoDeBaja; }
}
