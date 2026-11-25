package cl.duoc.acis.auth.service;

import cl.duoc.acis.auth.exception.AcisException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * Comunicación entre microservicios: ms-auth le pregunta a ms-ordenes por el código del local
 * con GET /locales/codigo/{codigo}.
 */
@Component
public class DirectorioLocalesHttp implements DirectorioLocales {

    /** Solo se lee el id de la respuesta de ms-ordenes. */
    record LocalResumen(Long id, String nombre) {
    }

    private final RestClient cliente;

    public DirectorioLocalesHttp(RestClient.Builder builder, @Value("${acis.ordenes.url}") String urlOrdenes) {
        this.cliente = builder.baseUrl(urlOrdenes).build();
    }

    @Override
    public Optional<Long> buscarPorCodigo(String codigo) {
        try {
            LocalResumen local = cliente.get()
                    .uri("/locales/codigo/{codigo}", codigo)
                    .retrieve()
                    .body(LocalResumen.class);
            return Optional.ofNullable(local).map(LocalResumen::id);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (ResourceAccessException e) {
            // ms-ordenes apagado o sin red
            throw new AcisException.ServicioNoDisponible("No se pudo validar el código del local. Intenta más tarde");
        }
    }
}
