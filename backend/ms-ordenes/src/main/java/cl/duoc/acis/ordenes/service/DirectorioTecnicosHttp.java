package cl.duoc.acis.ordenes.service;

import cl.duoc.acis.ordenes.exception.AcisException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Optional;

/**
 * Comunicación entre microservicios: ms-ordenes le pide a ms-auth la lista de técnicos activos
 * (GET /usuarios/tecnicos), reenviando el token del jefe que hizo el pedido.
 */
@Component
public class DirectorioTecnicosHttp implements DirectorioTecnicos {

    record Tecnico(Long id, String nombre) {
    }

    private final RestClient cliente;

    public DirectorioTecnicosHttp(RestClient.Builder builder, @Value("${acis.auth.url}") String urlAuth) {
        this.cliente = builder.baseUrl(urlAuth).build();
    }

    @Override
    public Optional<String> nombreDe(Long tecnicoId, String tokenDelJefe) {
        try {
            List<Tecnico> tecnicos = cliente.get()
                    .uri("/usuarios/tecnicos")
                    .header("Authorization", "Bearer " + tokenDelJefe)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Tecnico>>() { });
            return tecnicos == null ? Optional.empty() : tecnicos.stream()
                    .filter(t -> t.id().equals(tecnicoId))
                    .map(Tecnico::nombre)
                    .findFirst();
        } catch (RestClientException e) {
            throw new AcisException.ServicioNoDisponible("No se pudo consultar el técnico. Intenta más tarde");
        }
    }
}
