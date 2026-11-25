package cl.duoc.acis.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Código de 6 dígitos para recuperar la contraseña.
 * Reglas: vence en 10 minutos, sirve una sola vez y se invalida con 5 intentos fallidos.
 */
@Entity
@Table(name = "codigos_recuperacion")
public class CodigoRecuperacion {

    public static final int MAX_INTENTOS = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Usuario usuario;

    @Column(nullable = false, length = 6)
    private String codigo;

    @Column(nullable = false)
    private Instant venceEl;

    private int intentos;

    private boolean usado;

    protected CodigoRecuperacion() {
    }

    public CodigoRecuperacion(Usuario usuario, String codigo, Instant venceEl) {
        this.usuario = usuario;
        this.codigo = codigo;
        this.venceEl = venceEl;
    }

    /** Sirve si no se usó, no venció y no se agotaron los intentos. */
    public boolean sirve(Instant ahora) {
        return !usado && ahora.isBefore(venceEl) && intentos < MAX_INTENTOS;
    }

    /** Suma un intento fallido; al llegar al máximo el código deja de servir. */
    public void registrarIntentoFallido() {
        intentos++;
        if (intentos >= MAX_INTENTOS) {
            usado = true;
        }
    }

    public void marcarUsado() {
        usado = true;
    }

    public boolean coincide(String intento) {
        return codigo.equals(intento);
    }

    public Usuario getUsuario() { return usuario; }
    public int getIntentos() { return intentos; }
}
