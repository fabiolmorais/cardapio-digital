package br.com.cardapio.adapter.in.web.account;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * O cookie do refresh token.
 * - HttpOnly: o JavaScript da página não lê (um script injetado não rouba o token).
 * - Secure: só vai por HTTPS (o navegador aceita http://localhost como exceção).
 * - SameSite=Strict: outro site não consegue fazer o navegador enviá-lo.
 * - Path=/api/v1/auth: só vai para renovar e sair, não em toda chamada da API.
 */
@Component
class RefreshTokenCookie {

    static final String NAME = "refresh_token";
    static final String PATH = "/api/v1/auth";

    private final boolean secure;
    private final Clock clock;

    RefreshTokenCookie(@Value("${app.security.refresh-token.cookie-secure:true}") boolean secure, Clock clock) {
        this.secure = secure;
        this.clock = clock;
    }

    ResponseCookie create(String value, Instant expiresAt) {
        return base(value).maxAge(Duration.between(clock.instant(), expiresAt)).build();
    }

    /** Max-Age=0 manda o navegador apagar o cookie. */
    ResponseCookie clear() {
        return base("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(PATH);
    }
}
