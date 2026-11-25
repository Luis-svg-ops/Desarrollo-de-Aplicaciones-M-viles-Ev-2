package cl.duoc.acis.auth.exception;

import org.springframework.http.HttpStatus;

/**
 * Error de negocio de ACIS. Es abstracta: cada tipo de error es una subclase (herencia)
 * que responde con su propio código HTTP (polimorfismo). El manejador de errores no necesita
 * saber cuál es: solo llama a estadoHttp().
 */
public abstract class AcisException extends RuntimeException {

    protected AcisException(String mensaje) {
        super(mensaje);
    }

    public abstract HttpStatus estadoHttp();

    /** 400: los datos no cumplen una regla (ej. código de local inexistente). */
    public static class DatoInvalido extends AcisException {
        public DatoInvalido(String mensaje) { super(mensaje); }
        @Override public HttpStatus estadoHttp() { return HttpStatus.BAD_REQUEST; }
    }

    /** 401: correo o contraseña incorrectos. */
    public static class CredencialesInvalidas extends AcisException {
        public CredencialesInvalidas() { super("Correo o contraseña incorrectos"); }
        @Override public HttpStatus estadoHttp() { return HttpStatus.UNAUTHORIZED; }
    }

    /** 403: la cuenta existe pero no puede entrar (pendiente o inactiva). */
    public static class CuentaNoActiva extends AcisException {
        public CuentaNoActiva(String mensaje) { super(mensaje); }
        @Override public HttpStatus estadoHttp() { return HttpStatus.FORBIDDEN; }
    }

    /** 404: no existe lo que se busca. */
    public static class NoEncontrado extends AcisException {
        public NoEncontrado(String mensaje) { super(mensaje); }
        @Override public HttpStatus estadoHttp() { return HttpStatus.NOT_FOUND; }
    }

    /** 409: choca con algo que ya existe (ej. correo repetido). */
    public static class Conflicto extends AcisException {
        public Conflicto(String mensaje) { super(mensaje); }
        @Override public HttpStatus estadoHttp() { return HttpStatus.CONFLICT; }
    }

    /** 503: otro servicio del que dependemos no responde (ej. ms-ordenes apagado). */
    public static class ServicioNoDisponible extends AcisException {
        public ServicioNoDisponible(String mensaje) { super(mensaje); }
        @Override public HttpStatus estadoHttp() { return HttpStatus.SERVICE_UNAVAILABLE; }
    }
}
