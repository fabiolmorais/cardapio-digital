package br.com.cardapio.adapter.in.web.account;

import br.com.cardapio.application.port.in.account.CurrentAccount;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "Me")
record MeResponse(
        @Schema(requiredMode = REQUIRED) UUID userId,
        @Schema(requiredMode = REQUIRED) String name,
        @Schema(description = "Vazio para quem entra só por PIN (garçom, cozinha)") String email,
        @Schema(requiredMode = REQUIRED) EstablishmentAccessResponse establishment
) {
    static MeResponse from(CurrentAccount a) {
        return new MeResponse(a.userId().value(), a.name(), a.email() == null ? null : a.email().value(),
                EstablishmentAccessResponse.from(a.establishment()));
    }
}
