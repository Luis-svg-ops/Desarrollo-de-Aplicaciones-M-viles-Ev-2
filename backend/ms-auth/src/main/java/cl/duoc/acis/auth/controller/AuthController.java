package cl.duoc.acis.auth.controller;

import cl.duoc.acis.auth.dto.AuthDtos.LoginRequest;
import cl.duoc.acis.auth.dto.AuthDtos.LoginResponse;
import cl.duoc.acis.auth.dto.AuthDtos.MensajeResponse;
import cl.duoc.acis.auth.dto.AuthDtos.RecuperarRequest;
import cl.duoc.acis.auth.dto.AuthDtos.RegistroRequest;
import cl.duoc.acis.auth.dto.AuthDtos.RestablecerRequest;
import cl.duoc.acis.auth.service.AuthService;
import cl.duoc.acis.auth.service.RecuperacionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints públicos (no piden token). El controller solo recibe el pedido y responde:
 * las reglas están en los servicios.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final RecuperacionService recuperacion;

    public AuthController(AuthService authService, RecuperacionService recuperacion) {
        this.authService = authService;
        this.recuperacion = recuperacion;
    }

    /** POST /auth/login → token + datos del usuario. */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest pedido) {
        return authService.login(pedido);
    }

    /** POST /auth/registro → crea un cliente pendiente de aprobación (201 Created). */
    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public MensajeResponse registro(@Valid @RequestBody RegistroRequest pedido) {
        authService.registrar(pedido);
        return new MensajeResponse("Registro recibido. Te avisaremos por correo cuando el administrador apruebe tu cuenta");
    }

    /** POST /auth/recuperar → envía un código. Siempre responde lo mismo. */
    @PostMapping("/recuperar")
    public MensajeResponse recuperar(@Valid @RequestBody RecuperarRequest pedido) {
        recuperacion.pedirCodigo(pedido.correo());
        return new MensajeResponse(RecuperacionService.MENSAJE_NEUTRO);
    }

    /** POST /auth/restablecer → código + contraseña nueva. */
    @PostMapping("/restablecer")
    public MensajeResponse restablecer(@Valid @RequestBody RestablecerRequest pedido) {
        recuperacion.restablecer(pedido);
        return new MensajeResponse("Contraseña actualizada. Ya puedes iniciar sesión");
    }
}
