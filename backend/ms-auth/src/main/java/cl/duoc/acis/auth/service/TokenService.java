package cl.duoc.acis.auth.service;

import cl.duoc.acis.auth.model.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Crea el token JWT: un "pase" firmado con el secreto del servidor.
 * Lleva quién es (sub = id), su rol y, si es cliente, su local. ms-ordenes lee estos datos
 * para filtrar, en vez de confiar en lo que mande la app.
 */
@Service
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final long duracionSegundos;

    public TokenService(JwtEncoder jwtEncoder, @Value("${jwt.duracion-segundos}") long duracionSegundos) {
        this.jwtEncoder = jwtEncoder;
        this.duracionSegundos = duracionSegundos;
    }

    public String crear(Usuario usuario) {
        Instant ahora = Instant.now();
        JwtClaimsSet.Builder datos = JwtClaimsSet.builder()
                .issuer("ms-auth")
                .issuedAt(ahora)
                .expiresAt(ahora.plusSeconds(duracionSegundos))
                .subject(String.valueOf(usuario.getId()))
                .claim("nombre", usuario.getNombre())
                .claim("rol", usuario.getRol().name());
        if (usuario.getLocalId() != null) {
            datos.claim("localId", usuario.getLocalId());
        }
        return jwtEncoder.encode(JwtEncoderParameters.from(datos.build())).getTokenValue();
    }

    public long getDuracionSegundos() {
        return duracionSegundos;
    }
}
