package br.com.cardapio.adapter.in.web.account;

import br.com.cardapio.application.port.in.account.SignupResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "SignupResponse")
record SignupResponse(
        @Schema(requiredMode = REQUIRED) UUID organizationId,
        @Schema(requiredMode = REQUIRED) UUID establishmentId,
        @Schema(requiredMode = REQUIRED) UUID userId,
        @Schema(requiredMode = REQUIRED, example = "sakura-sushi") String slug
) {

    static SignupResponse from(SignupResult result) {
        return new SignupResponse(result.organizationId().value(), result.establishmentId().value(),
                result.userId().value(), result.slug());
    }
}
