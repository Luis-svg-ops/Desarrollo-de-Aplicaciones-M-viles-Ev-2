package cl.duoc.acis.auth.correo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Correo simulado: se escribe en la consola del backend. Es el modo por defecto. */
@Component
@ConditionalOnProperty(name = "acis.correo.modo", havingValue = "consola", matchIfMissing = true)
public class CorreoConsola implements EnviadorCorreo {

    private static final Logger log = LoggerFactory.getLogger(CorreoConsola.class);

    @Override
    public void enviar(String destinatario, String asunto, String texto) {
        log.info("""

                ===== CORREO (simulado) =====
                Para:   {}
                Asunto: {}
                {}
                =============================""", destinatario, asunto, texto);
    }
}
