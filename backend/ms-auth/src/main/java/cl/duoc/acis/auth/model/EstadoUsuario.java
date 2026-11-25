package cl.duoc.acis.auth.model;

public enum EstadoUsuario {
    /** Cliente recién registrado: no puede iniciar sesión hasta que el Admin lo apruebe. */
    PENDIENTE,
    ACTIVO,
    /** Desactivado por el Admin: no puede iniciar sesión (se conserva por el historial de las OT). */
    INACTIVO
}
