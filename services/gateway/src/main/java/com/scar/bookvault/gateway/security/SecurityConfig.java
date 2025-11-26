package com.scar.bookvault.gateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Configuration
@EnableReactiveMethodSecurity
public class SecurityConfig {
    @Value("${security.jwt.publicKeyPem:}")
    private String publicKeyPem;

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http.csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(ex -> ex
                .pathMatchers("/actuator/**", "/v3/api-docs/**", "/swagger-ui/**", "/api/iam/v1/auth/**").permitAll()
                .pathMatchers("/api/**").authenticated()
                .anyExchange().permitAll()
            )
            .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtDecoder(jwtDecoder())));
        return http.build();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        try {
            String pem = publicKeyPem.replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "").replaceAll("\\s", "");
            byte[] decoded = Base64.getDecoder().decode(pem);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PublicKey key = kf.generatePublic(spec);
            return NimbusReactiveJwtDecoder.withPublicKey(key).build();
        } catch (Exception e) {
            return NimbusReactiveJwtDecoder.withPublicKey(null).build();
        }
    }
}

