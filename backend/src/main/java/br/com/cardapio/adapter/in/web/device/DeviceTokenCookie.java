package br.com.cardapio.adapter.in.web.device;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * O cookie com o token do aparelho. Mesmas proteções do refresh token (HttpOnly, Secure,
 * SameSite=Strict), mas vida longa: o aparelho fica vinculado até o gerente desvincular.
 * Path=/api/v1/auth: só vai para o login por PIN (Guia 6), não em toda chamada da API.
 */
@Component
public class DeviceTokenCookie {

    public static final String NAME = "device_token";
    static final String PATH = "/api/v1/auth";
    /** O Chrome não aceita cookie com validade maior que 400 dias. */
    static final Duration MAX_AGE = Duration.ofDays(400);

    private final boolean secure;

    /** Mesma chave do refresh token: os dois cookies seguem a mesma regra de HTTPS. */
    DeviceTokenCookie(@Value("${app.security.refresh-token.cookie-secure:true}") boolean secure) {
        this.secure = secure;
    }

    ResponseCookie create(String value) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(PATH)
                .maxAge(MAX_AGE)
                .build();
    }
}
