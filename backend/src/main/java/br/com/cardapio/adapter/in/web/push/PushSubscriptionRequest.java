package br.com.cardapio.adapter.in.web.push;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/** A inscrição como o navegador a entrega (PushSubscription.toJSON()). */
@Schema(name = "PushSubscriptionRequest")
record PushSubscriptionRequest(
        @Schema(requiredMode = REQUIRED, description = "O endereço do serviço de push do navegador")
        @NotBlank @Size(max = 1000) String endpoint,
        @Schema(requiredMode = REQUIRED) @NotNull @Valid Keys keys
) {

    @Schema(name = "PushSubscriptionKeys")
    record Keys(
            @Schema(requiredMode = REQUIRED, description = "A chave pública do navegador, em base64url")
            @NotBlank @Size(max = 100) String p256dh,
            @Schema(requiredMode = REQUIRED, description = "O segredo do navegador, em base64url")
            @NotBlank @Size(max = 40) String auth
    ) {
    }
}
