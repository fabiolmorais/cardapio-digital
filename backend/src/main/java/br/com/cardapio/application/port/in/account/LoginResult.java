package br.com.cardapio.application.port.in.account;

import br.com.cardapio.domain.account.model.UserId;

import java.time.Instant;

public record LoginResult(
        String accessToken,
        Instant expiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt,
        UserId userId,
        String userName,
        EstablishmentAccess establishment
) {

    @Override
    public String toString() {
        return "LoginResult[userId=" + userId + "]";
    }
}
