package cl.duoc.acis.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;

/**
 * Quién puede llamar a cada endpoint. El servidor no guarda sesiones (STATELESS):
 * cada pedido trae su token JWT y aquí se verifica la firma y el rol.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Públicos: login, registro y recuperar contraseña
                        .requestMatchers(HttpMethod.POST, "/auth/**").permitAll()
                        // Solo el Administrador gestiona usuarios
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // El jefe necesita la lista de técnicos para asignar OT
                        .requestMatchers(HttpMethod.GET, "/usuarios/tecnicos").hasAnyRole("JEFE", "ADMIN")
                        // Perfil propio: cualquiera con sesión
                        .requestMatchers("/usuarios/yo").authenticated()
                        .anyRequest().denyAll())
                .oauth2ResourceServer(rs -> rs.jwt(jwt -> jwt.jwtAuthenticationConverter(convertidorDeRol())))
                .build();
    }

    /** El claim "rol" del token (ej. ADMIN) se convierte en el permiso ROLE_ADMIN de Spring Security. */
    private Converter<Jwt, ? extends AbstractAuthenticationToken> convertidorDeRol() {
        return jwt -> {
            String rol = jwt.getClaimAsString("rol");
            List<SimpleGrantedAuthority> permisos = rol == null
                    ? List.of()
                    : List.of(new SimpleGrantedAuthority("ROLE_" + rol));
            return new JwtAuthenticationToken(jwt, permisos, jwt.getSubject());
        };
    }

    /** Cifra las contraseñas con BCrypt: aunque alguien vea la base, no ve las claves. */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private SecretKey llave(String secreto) {
        return new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    /** Firma los tokens que entrega el login. */
    @Bean
    JwtEncoder jwtEncoder(@Value("${jwt.secret}") String secreto) {
        return NimbusJwtEncoder.withSecretKey(llave(secreto)).algorithm(MacAlgorithm.HS256).build();
    }

    /** Verifica la firma y el vencimiento de los tokens que llegan. */
    @Bean
    JwtDecoder jwtDecoder(@Value("${jwt.secret}") String secreto) {
        return NimbusJwtDecoder.withSecretKey(llave(secreto)).macAlgorithm(MacAlgorithm.HS256).build();
    }

    /** Reloj del sistema (en las pruebas se puede reemplazar para simular que pasó el tiempo). */
    @Bean
    Clock reloj() {
        return Clock.systemDefaultZone();
    }
}
