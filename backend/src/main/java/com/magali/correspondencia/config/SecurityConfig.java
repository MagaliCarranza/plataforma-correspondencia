package com.magali.correspondencia.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Valor por defecto del secreto JWT, solo apto para desarrollo. Si este valor llega al
     * perfil {@code prod} significa que nadie definio CORRESPONDENCIA_SEGURIDAD_JWT_SECRETO:
     * el arranque falla antes de aceptar trafico.
     */
    static final String JWT_SECRETO_DEFAULT_DEV =
            "desarrollo-cambia-este-secreto-en-produccion-por-favor-32chars";
    static final int JWT_SECRETO_LONGITUD_MINIMA_BYTES = 32;

    private final String jwtSecret;
    private final List<String> corsOrigins;
    private final Environment environment;

    public SecurityConfig(
            @Value("${correspondencia.seguridad.jwt-secreto}") String jwtSecret,
            @Value("${correspondencia.seguridad.cors-origins}") List<String> corsOrigins,
            Environment environment) {
        this.jwtSecret = jwtSecret;
        this.corsOrigins = corsOrigins;
        this.environment = environment;
    }

    @PostConstruct
    void validarSecretoJwt() {
        if (!environment.acceptsProfiles(Profiles.of("prod"))) {
            return;
        }
        if (JWT_SECRETO_DEFAULT_DEV.equals(jwtSecret)) {
            throw new IllegalStateException(
                    "El perfil prod esta activo pero CORRESPONDENCIA_SEGURIDAD_JWT_SECRETO conserva el valor "
                            + "por defecto de desarrollo. Define un secreto propio de al menos "
                            + JWT_SECRETO_LONGITUD_MINIMA_BYTES + " bytes antes de arrancar.");
        }
        int longitudBytes = jwtSecret.getBytes(StandardCharsets.UTF_8).length;
        if (longitudBytes < JWT_SECRETO_LONGITUD_MINIMA_BYTES) {
            throw new IllegalStateException(
                    "CORRESPONDENCIA_SEGURIDAD_JWT_SECRETO debe tener al menos "
                            + JWT_SECRETO_LONGITUD_MINIMA_BYTES + " bytes (HS256). Actual: "
                            + longitudBytes + " bytes.");
        }
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/demo/info").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
                        .jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(corsOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey()));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(secretKey())
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthorities = new JwtGrantedAuthoritiesConverter();
        grantedAuthorities.setAuthoritiesClaimName("rol");
        grantedAuthorities.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(grantedAuthorities);
        return converter;
    }

    private SecretKeySpec secretKey() {
        return new SecretKeySpec(jwtSecret.getBytes(), "HmacSHA256");
    }
}
