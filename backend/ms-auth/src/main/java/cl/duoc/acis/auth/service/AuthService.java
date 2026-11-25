package cl.duoc.acis.auth.service;

import cl.duoc.acis.auth.correo.EnviadorCorreo;
import cl.duoc.acis.auth.dto.AuthDtos.LoginRequest;
import cl.duoc.acis.auth.dto.AuthDtos.LoginResponse;
import cl.duoc.acis.auth.dto.AuthDtos.RegistroRequest;
import cl.duoc.acis.auth.dto.AuthDtos.UsuarioResponse;
import cl.duoc.acis.auth.exception.AcisException;
import cl.duoc.acis.auth.model.EstadoUsuario;
import cl.duoc.acis.auth.model.Rol;
import cl.duoc.acis.auth.model.Usuario;
import cl.duoc.acis.auth.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reglas del inicio de sesión y del registro de clientes. */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;
    private final DirectorioLocales locales;
    private final EnviadorCorreo correo;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder, TokenService tokens,
                       DirectorioLocales locales, EnviadorCorreo correo) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.locales = locales;
        this.correo = correo;
    }

    /** Correos sin espacios y en minúscula: "Pedro@ACIS.cl " y "pedro@acis.cl" son el mismo. */
    static String normalizar(String correo) {
        return correo.trim().toLowerCase();
    }

    /**
     * Login: primero se revisa la clave y después el estado. Así, sin la clave correcta
     * nadie puede averiguar si una cuenta está pendiente o desactivada.
     */
    public LoginResponse login(LoginRequest pedido) {
        Usuario usuario = usuarios.findByCorreo(normalizar(pedido.correo()))
                .filter(u -> passwordEncoder.matches(pedido.clave(), u.getClave()))
                .orElseThrow(AcisException.CredencialesInvalidas::new);

        switch (usuario.getEstado()) {
            case PENDIENTE -> throw new AcisException.CuentaNoActiva(
                    "Tu cuenta está pendiente de aprobación. Te avisaremos por correo cuando el administrador la confirme");
            case INACTIVO -> throw new AcisException.CuentaNoActiva(
                    "Tu cuenta está desactivada. Contacta al administrador");
            case ACTIVO -> { /* puede entrar */ }
        }

        return new LoginResponse(tokens.crear(usuario), "Bearer", tokens.getDuracionSegundos(),
                UsuarioResponse.de(usuario));
    }

    /**
     * Registro de clientes: siempre como CLIENTE y PENDIENTE (nadie se puede registrar como jefe o admin).
     * El código del local se valida con ms-ordenes, y se avisa a los administradores para que aprueben.
     */
    @Transactional
    public Usuario registrar(RegistroRequest pedido) {
        String correoNormalizado = normalizar(pedido.correo());
        if (usuarios.existsByCorreo(correoNormalizado)) {
            throw new AcisException.Conflicto("Ese correo ya está registrado");
        }
        Long localId = locales.buscarPorCodigo(pedido.codigoLocal().trim().toUpperCase())
                .orElseThrow(() -> new AcisException.DatoInvalido("Código de local no válido"));

        Usuario nuevo = usuarios.save(new Usuario(
                pedido.nombre().trim(), correoNormalizado, passwordEncoder.encode(pedido.clave()),
                pedido.telefono().trim(), Rol.CLIENTE, EstadoUsuario.PENDIENTE, localId));

        avisarAdministradores(nuevo);
        return nuevo;
    }

    private void avisarAdministradores(Usuario cliente) {
        for (Usuario admin : usuarios.findByRolAndEstadoOrderByNombre(Rol.ADMIN, EstadoUsuario.ACTIVO)) {
            try {
                correo.enviar(admin.getCorreo(), "ACIS: nuevo cliente por aprobar",
                        cliente.getNombre() + " (" + cliente.getCorreo() + ") se registró y espera tu aprobación.\n"
                                + "Revísalo en la app: Usuarios → Por aprobar.");
            } catch (AcisException e) {
                // Que falle el aviso no debe impedir el registro: el cliente igual queda en la lista
                log.warn("No se pudo avisar al administrador {}", admin.getCorreo());
            }
        }
    }
}
