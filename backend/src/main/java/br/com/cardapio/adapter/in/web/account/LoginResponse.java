package br.com.cardapio.adapter.in.web.account;

import br.com.cardapio.application.port.in.account.LoginResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "LoginResponse")
record LoginResponse(
        @Schema(requiredMode = REQUIRED, description = "JWT para o cabeçalho Authorization: Bearer") String accessToken,
        @Schema(requiredMode = REQUIRED, example = "Bearer") String tokenType,
        @Schema(requiredMode = REQUIRED) Instant expiresAt,
        @Schema(requiredMode = REQUIRED) UUID userId,
        @Schema(requiredMode = REQUIRED) String userName,
        @Schema(requiredMode = REQUIRED) EstablishmentAccessResponse establishment
) {
    static LoginResponse from(LoginResult r) {
        return new LoginResponse(r.accessToken(), "Bearer", r.expiresAt(),
                r.userId().value(), r.userName(), EstablishmentAccessResponse.from(r.establishment()));
    }

    @Override
    public String toString() {
        return "LoginResponse[userId=" + userId + "]";
    }
}
