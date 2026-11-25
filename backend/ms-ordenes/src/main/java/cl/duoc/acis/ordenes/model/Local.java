package cl.duoc.acis.ordenes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Local del cliente. El "código" es el que ACIS le entrega al cliente para registrarse. */
@Entity
@Table(name = "locales")
public class Local {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(nullable = false, length = 80)
    private String direccion;

    @Column(nullable = false, length = 40)
    private String comuna;

    @Column(nullable = false, length = 60)
    private String nombreAdministrador;

    @Column(nullable = false, length = 20)
    private String telefono;

    /** Coordenadas del local (para mostrar el clima con la API externa). */
    private Double latitud;
    private Double longitud;

    protected Local() {
    }

    public Local(String codigo, String nombre, String direccion, String comuna, String nombreAdministrador,
                 String telefono, Double latitud, Double longitud) {
        actualizar(codigo, nombre, direccion, comuna, nombreAdministrador, telefono, latitud, longitud);
    }

    public final void actualizar(String codigo, String nombre, String direccion, String comuna,
                                 String nombreAdministrador, String telefono, Double latitud, Double longitud) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.direccion = direccion;
        this.comuna = comuna;
        this.nombreAdministrador = nombreAdministrador;
        this.telefono = telefono;
        this.latitud = latitud;
        this.longitud = longitud;
    }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public String getDireccion() { return direccion; }
    public String getComuna() { return comuna; }
    public String getNombreAdministrador() { return nombreAdministrador; }
    public String getTelefono() { return telefono; }
    public Double getLatitud() { return latitud; }
    public Double getLongitud() { return longitud; }
}
