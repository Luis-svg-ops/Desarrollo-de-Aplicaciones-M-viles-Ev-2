package cl.duoc.acis.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad: una fila de la tabla "usuarios" en la base H2.
 * Los atributos son privados y solo se cambian con métodos con nombre (encapsulamiento):
 * por ejemplo, la clave solo se guarda ya cifrada y el estado cambia con aprobar() o desactivar().
 */
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(nullable = false, unique = true, length = 80)
    private String correo;

    /** Hash BCrypt, nunca la clave escrita. */
    @Column(nullable = false)
    private String clave;

    @Column(length = 20)
    private String telefono;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Rol rol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoUsuario estado;

    /** Solo para clientes: el local al que pertenece. */
    private Long localId;

    protected Usuario() {
        // Lo necesita JPA
    }

    public Usuario(String nombre, String correo, String claveCifrada, String telefono,
                   Rol rol, EstadoUsuario estado, Long localId) {
        this.nombre = nombre;
        this.correo = correo;
        this.clave = claveCifrada;
        this.telefono = telefono;
        this.rol = rol;
        this.estado = estado;
        this.localId = localId;
    }

    public boolean estaActivo() {
        return estado == EstadoUsuario.ACTIVO;
    }

    public void aprobar() {
        this.estado = EstadoUsuario.ACTIVO;
    }

    public void desactivar() {
        this.estado = EstadoUsuario.INACTIVO;
    }

    public void cambiarClave(String claveCifrada) {
        this.clave = claveCifrada;
    }

    public void actualizarPerfil(String nombre, String telefono) {
        this.nombre = nombre;
        this.telefono = telefono;
    }

    /** Cambios que solo hace el Administrador. */
    public void actualizarComoAdmin(String nombre, String telefono, Rol rol, EstadoUsuario estado, Long localId) {
        this.nombre = nombre;
        this.telefono = telefono;
        this.rol = rol;
        this.estado = estado;
        this.localId = localId;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getCorreo() { return correo; }
    public String getClave() { return clave; }
    public String getTelefono() { return telefono; }
    public Rol getRol() { return rol; }
    public EstadoUsuario getEstado() { return estado; }
    public Long getLocalId() { return localId; }
}
