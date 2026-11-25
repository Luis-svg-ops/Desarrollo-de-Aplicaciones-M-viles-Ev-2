package cl.duoc.acis.ordenes.service;

import cl.duoc.acis.ordenes.model.Rol;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Quién está haciendo el pedido, leído desde el token firmado por ms-auth.
 * Nunca se usa un id, rol o local que mande la app: solo lo que viene dentro del token.
 */
public record UsuarioActual(Long id, String nombre, Rol rol, Long localId, String token) {

    public static UsuarioActual desde(Jwt jwt) {
        Object local = jwt.getClaim("localId");
        return new UsuarioActual(
                Long.valueOf(jwt.getSubject()),
                jwt.getClaimAsString("nombre"),
                Rol.valueOf(jwt.getClaimAsString("rol")),
                local == null ? null : Long.valueOf(local.toString()),
                jwt.getTokenValue());
    }

    public boolean veTodo() {
        return rol == Rol.JEFE || rol == Rol.ADMIN;
    }
}
