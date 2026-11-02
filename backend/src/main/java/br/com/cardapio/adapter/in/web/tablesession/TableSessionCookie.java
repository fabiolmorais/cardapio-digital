package br.com.cardapio.adapter.in.web.tablesession;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * O cookie da sessão da mesa, com os cuidados do refresh token (Guia 3):
 * - HttpOnly: o JavaScript da página não lê;
 * - Secure: só por HTTPS (mesma chave do refresh: false só para testar pela rede local, em http);
 * - SameSite=Strict: outro site não faz o navegador enviá-lo;
 * - Path=/api/v1/public: só vai para a API da página da mesa.
 */
@Component
class TableSessionCookie {

    static final String NAME = "table_session";
    static final String PATH = "/api/v1/public";

    private final boolean secure;
    private final Clock clock;

    TableSessionCookie(@Value("${app.security.refresh-token.cookie-secure:true}") boolean secure, Clock clock) {
        this.secure = secure;
        this.clock = clock;
    }

    ResponseCookie create(String value, Instant expiresAt) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(PATH)
                .maxAge(Duration.between(clock.instant(), expiresAt))
                .build();
    }
}
