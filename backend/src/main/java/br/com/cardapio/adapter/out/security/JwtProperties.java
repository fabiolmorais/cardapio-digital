package br.com.cardapio.adapter.out.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Configuração do JWT (app.security.jwt.*). A chave vem da variável de ambiente JWT_SECRET.
 */
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        String secret,
        String issuer,
        Duration accessTokenTtl
) {

    /** HS256 exige chave de pelo menos 256 bits. */
    public static final int MIN_SECRET_BYTES = 32;

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "Defina JWT_SECRET com pelo menos " + MIN_SECRET_BYTES + " bytes (openssl rand -base64 48)");
        }
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalStateException("Defina app.security.jwt.issuer");
        }
        if (accessTokenTtl == null || accessTokenTtl.isNegative() || accessTokenTtl.isZero()) {
            throw new IllegalStateException("Defina app.security.jwt.access-token-ttl");
        }
    }
}
