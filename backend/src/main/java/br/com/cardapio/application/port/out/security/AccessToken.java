package br.com.cardapio.application.port.out.security;

import java.time.Instant;

public record AccessToken(String value, Instant expiresAt) {

    @Override
    public String toString() {
        return "AccessToken[expiresAt=" + expiresAt + "]";
    }
}
