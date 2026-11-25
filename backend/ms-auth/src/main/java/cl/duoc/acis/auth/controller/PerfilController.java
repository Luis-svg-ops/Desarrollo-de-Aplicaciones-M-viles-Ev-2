package cl.duoc.acis.auth.controller;

import cl.duoc.acis.auth.dto.AuthDtos.PerfilRequest;
import cl.duoc.acis.auth.dto.AuthDtos.UsuarioResponse;
import cl.duoc.acis.auth.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints con sesión. El id del usuario se saca del token (jwt.getSubject()),
 * nunca de la URL: así nadie puede editar el perfil de otro cambiando un número.
 */
@RestController
@RequestMapping("/usuarios")
public class PerfilController {

    private final UsuarioService usuarios;

    public PerfilController(UsuarioService usuarios) {
        this.usuarios = usuarios;
    }

    static Long idDelToken(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }

    /** GET /usuarios/yo → mi perfil. */
    @GetMapping("/yo")
    public UsuarioResponse miPerfil(@AuthenticationPrincipal Jwt jwt) {
        return UsuarioResponse.de(usuarios.buscar(idDelToken(jwt)));
    }

    /** PUT /usuarios/yo → editar mi nombre, teléfono y (opcional) contraseña. */
    @PutMapping("/yo")
    public UsuarioResponse editarPerfil(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PerfilRequest pedido) {
        return UsuarioResponse.de(usuarios.actualizarPerfil(idDelToken(jwt), pedido));
    }

    /** GET /usuarios/tecnicos → técnicos activos (jefe y admin). */
    @GetMapping("/tecnicos")
    public List<UsuarioResponse> tecnicos() {
        return usuarios.tecnicosActivos().stream().map(UsuarioResponse::de).toList();
    }
}
