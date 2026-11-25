package cl.duoc.acis.auth.controller;

import cl.duoc.acis.auth.dto.AuthDtos.CrearUsuarioRequest;
import cl.duoc.acis.auth.dto.AuthDtos.EditarUsuarioRequest;
import cl.duoc.acis.auth.dto.AuthDtos.MensajeResponse;
import cl.duoc.acis.auth.dto.AuthDtos.UsuarioResponse;
import cl.duoc.acis.auth.model.EstadoUsuario;
import cl.duoc.acis.auth.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** CRUD de usuarios. Solo ADMIN (lo exige SecurityConfig en /admin/**). */
@RestController
@RequestMapping("/admin/usuarios")
public class AdminUsuarioController {

    private final UsuarioService usuarios;

    public AdminUsuarioController(UsuarioService usuarios) {
        this.usuarios = usuarios;
    }

    /** GET /admin/usuarios?estado=PENDIENTE → lista (filtro opcional). */
    @GetMapping
    public List<UsuarioResponse> listar(@RequestParam(required = false) EstadoUsuario estado) {
        return usuarios.listar(estado).stream().map(UsuarioResponse::de).toList();
    }

    @GetMapping("/{id}")
    public UsuarioResponse ver(@PathVariable Long id) {
        return UsuarioResponse.de(usuarios.buscar(id));
    }

    /** POST /admin/usuarios → crear técnico, jefe, admin o cliente (queda activo). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse crear(@Valid @RequestBody CrearUsuarioRequest pedido) {
        return UsuarioResponse.de(usuarios.crear(pedido));
    }

    /** PUT /admin/usuarios/{id} → editar datos, rol, estado o local. */
    @PutMapping("/{id}")
    public UsuarioResponse editar(@PathVariable Long id, @Valid @RequestBody EditarUsuarioRequest pedido,
                                  @AuthenticationPrincipal Jwt jwt) {
        return UsuarioResponse.de(usuarios.editar(id, pedido, PerfilController.idDelToken(jwt)));
    }

    /** PUT /admin/usuarios/{id}/aprobar → aprobar un cliente pendiente. */
    @PutMapping("/{id}/aprobar")
    public UsuarioResponse aprobar(@PathVariable Long id) {
        return UsuarioResponse.de(usuarios.aprobar(id));
    }

    /** DELETE /admin/usuarios/{id} → rechaza un pendiente (lo borra) o desactiva a un usuario existente. */
    @DeleteMapping("/{id}")
    public MensajeResponse eliminar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        boolean borrado = usuarios.eliminar(id, PerfilController.idDelToken(jwt));
        return new MensajeResponse(borrado
                ? "Registro rechazado y eliminado"
                : "Usuario desactivado (se conserva por el historial de las OT)");
    }
}
