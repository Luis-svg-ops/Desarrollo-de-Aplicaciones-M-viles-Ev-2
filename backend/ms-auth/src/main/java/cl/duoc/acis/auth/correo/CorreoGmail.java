package cl.duoc.acis.auth.correo;

import cl.duoc.acis.auth.exception.AcisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Correo real por Gmail (SMTP). Se activa con CORREO_MODO=gmail.
 * Necesita GMAIL_USUARIO y GMAIL_CLAVE_APP (una "contraseña de aplicación" de Google),
 * que se pasan como variables de entorno y nunca se suben al repo.
 */
@Component
@ConditionalOnProperty(name = "acis.correo.modo", havingValue = "gmail")
public class CorreoGmail implements EnviadorCorreo {

    private static final Logger log = LoggerFactory.getLogger(CorreoGmail.class);

    private final JavaMailSender mailSender;
    private final String remitente;

    public CorreoGmail(JavaMailSender mailSender, @Value("${acis.correo.remitente}") String remitente) {
        this.mailSender = mailSender;
        this.remitente = remitente;
    }

    @Override
    public void enviar(String destinatario, String asunto, String texto) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(destinatario);
        mensaje.setSubject(asunto);
        mensaje.setText(texto);
        try {
            mailSender.send(mensaje);
        } catch (MailException e) {
            // Se registra el detalle técnico y se responde un mensaje claro
            log.error("No se pudo enviar el correo a {}", destinatario, e);
            throw new AcisException.ServicioNoDisponible("No se pudo enviar el correo. Intenta más tarde");
        }
    }
}
