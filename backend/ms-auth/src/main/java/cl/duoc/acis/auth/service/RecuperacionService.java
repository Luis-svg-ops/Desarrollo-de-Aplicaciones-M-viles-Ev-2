package cl.duoc.acis.auth.service;

import cl.duoc.acis.auth.correo.EnviadorCorreo;
import cl.duoc.acis.auth.dto.AuthDtos.RestablecerRequest;
import cl.duoc.acis.auth.exception.AcisException;
import cl.duoc.acis.auth.model.CodigoRecuperacion;
import cl.duoc.acis.auth.model.Usuario;
import cl.duoc.acis.auth.repository.CodigoRecuperacionRepository;
import cl.duoc.acis.auth.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Recuperar contraseña en 2 pasos: pedir un código (llega por correo) y usarlo para poner una clave nueva.
 * Seguridad: el código vence en 10 minutos, sirve una vez y se invalida con 5 intentos fallidos.
 */
@Service
public class RecuperacionService {

    static final Duration VIGENCIA = Duration.ofMinutes(10);

    /** Respuesta neutra: es la misma exista o no el correo, para no revelar quién tiene cuenta. */
    public static final String MENSAJE_NEUTRO = "Si el correo está registrado, te llegó un código de 6 dígitos";

    private static final String CODIGO_INVALIDO = "Código inválido o vencido. Pide uno nuevo";

    private final UsuarioRepository usuarios;
    private final CodigoRecuperacionRepository codigos;
    private final PasswordEncoder passwordEncoder;
    private final EnviadorCorreo correo;
    private final Clock reloj;
    private final SecureRandom azar = new SecureRandom();

    public RecuperacionService(UsuarioRepository usuarios, CodigoRecuperacionRepository codigos,
                               PasswordEncoder passwordEncoder, EnviadorCorreo correo, Clock reloj) {
        this.usuarios = usuarios;
        this.codigos = codigos;
        this.passwordEncoder = passwordEncoder;
        this.correo = correo;
        this.reloj = reloj;
    }

    /** Paso 1. Solo las cuentas activas reciben código, pero la respuesta siempre es la misma. */
    @Transactional
    public void pedirCodigo(String correoPedido) {
        usuarios.findByCorreo(AuthService.normalizar(correoPedido))
                .filter(Usuario::estaActivo)
                .ifPresent(this::generarYEnviar);
    }

    private void generarYEnviar(Usuario usuario) {
        // Un código nuevo anula los anteriores
        codigos.findByUsuarioAndUsadoFalse(usuario).forEach(CodigoRecuperacion::marcarUsado);

        String codigo = String.format("%06d", azar.nextInt(1_000_000));
        codigos.save(new CodigoRecuperacion(usuario, codigo, Instant.now(reloj).plus(VIGENCIA)));

        correo.enviar(usuario.getCorreo(), "ACIS: código para recuperar tu contraseña",
                "Hola " + usuario.getNombre() + ",\n\nTu código es: " + codigo + "\n"
                        + "Vence en 10 minutos y sirve una sola vez.\n"
                        + "Si no lo pediste, ignora este correo.");
    }

    /** Paso 2. Cualquier problema responde lo mismo, salvo que el código sea incorrecto. */
    @Transactional(noRollbackFor = AcisException.class)
    public void restablecer(RestablecerRequest pedido) {
        Usuario usuario = usuarios.findByCorreo(AuthService.normalizar(pedido.correo()))
                .orElseThrow(() -> new AcisException.DatoInvalido(CODIGO_INVALIDO));

        CodigoRecuperacion vigente = codigos.findFirstByUsuarioOrderByIdDesc(usuario)
                .filter(c -> c.sirve(Instant.now(reloj)))
                .orElseThrow(() -> new AcisException.DatoInvalido(CODIGO_INVALIDO));

        if (!vigente.coincide(pedido.codigo())) {
            // El intento fallido se guarda aunque se responda con error (por eso noRollbackFor)
            vigente.registrarIntentoFallido();
            int quedan = CodigoRecuperacion.MAX_INTENTOS - vigente.getIntentos();
            throw new AcisException.DatoInvalido(quedan > 0
                    ? "Código incorrecto. Te quedan " + quedan + " intentos"
                    : "Código incorrecto. Se agotaron los intentos: pide uno nuevo");
        }

        vigente.marcarUsado();
        usuario.cambiarClave(passwordEncoder.encode(pedido.claveNueva()));
    }
}
