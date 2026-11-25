package cl.duoc.acis.ordenes.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;

/**
 * Quién puede llamar a cada endpoint de ms-ordenes. Este servicio no firma tokens:
 * solo verifica los que firma ms-auth (con el mismo secreto), igual que ms-products en el ejemplo del profe.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Público: ms-auth valida aquí el código del local cuando un cliente se registra
                        .requestMatchers(HttpMethod.GET, "/locales/codigo/*").permitAll()
                        // El cliente ve su propio local
                        .requestMatchers(HttpMethod.GET, "/locales/mio").authenticated()
                        // Locales y equipos los gestionan el jefe y el admin
                        .requestMatchers("/locales/**", "/equipos/**").hasAnyRole("JEFE", "ADMIN")
                        // Crear, editar y eliminar OT: solo el jefe
                        .requestMatchers(HttpMethod.POST, "/ordenes").hasRole("JEFE")
                        .requestMatchers(HttpMethod.PUT, "/ordenes/*").hasRole("JEFE")
                        .requestMatchers(HttpMethod.DELETE, "/ordenes/*").hasRole("JEFE")
                        // Ver OT y hacer acciones: cualquiera con sesión. El servicio filtra por rol
                        // y la máquina de estados decide qué puede hacer cada uno
                        .requestMatchers("/ordenes/**").authenticated()
                        .anyRequest().denyAll())
                .oauth2ResourceServer(rs -> rs.jwt(jwt -> jwt.jwtAuthenticationConverter(convertidorDeRol())))
                .build();
    }

    /** El claim "rol" del token (ej. JEFE) se convierte en el permiso ROLE_JEFE de Spring Security. */
    private Converter<Jwt, ? extends AbstractAuthenticationToken> convertidorDeRol() {
        return jwt -> {
            String rol = jwt.getClaimAsString("rol");
            List<SimpleGrantedAuthority> permisos = rol == null
                    ? List.of()
                    : List.of(new SimpleGrantedAuthority("ROLE_" + rol));
            return new JwtAuthenticationToken(jwt, permisos, jwt.getSubject());
        };
    }

    /** Verifica la firma y el vencimiento de los tokens que llegan. */
    @Bean
    JwtDecoder jwtDecoder(@Value("${jwt.secret}") String secreto) {
        SecretKeySpec llave = new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(llave).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    Clock reloj() {
        return Clock.systemDefaultZone();
    }
}
