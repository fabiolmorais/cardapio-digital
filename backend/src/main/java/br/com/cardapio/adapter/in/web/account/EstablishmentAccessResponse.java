package br.com.cardapio.adapter.in.web.account;

import br.com.cardapio.application.port.in.account.EstablishmentAccess;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/** Estabelecimento em que a pessoa está trabalhando, com o papel dela ali. */
@Schema(name = "EstablishmentAccess")
record EstablishmentAccessResponse(
        @Schema(requiredMode = REQUIRED) UUID id,
        @Schema(requiredMode = REQUIRED, example = "Sakura Sushi") String name,
        @Schema(requiredMode = REQUIRED, example = "sakura-sushi") String slug,
        @Schema(requiredMode = REQUIRED) RoleDto role
) {

    enum RoleDto { OWNER, MANAGER, STAFF, KITCHEN }

    static EstablishmentAccessResponse from(EstablishmentAccess a) {
        return new EstablishmentAccessResponse(a.establishmentId().value(), a.name(), a.slug(),
                RoleDto.valueOf(a.role().name()));
    }
}
