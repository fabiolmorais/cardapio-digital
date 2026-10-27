package br.com.cardapio.adapter.in.web.device;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "ClaimPairingRequest")
record ClaimPairingRequest(
        @Schema(requiredMode = REQUIRED, description = "O segredo recebido ao abrir o pedido") @NotBlank @Size(max = 100)
        String secret
) {
    @Override
    public String toString() {
        return "ClaimPairingRequest[]";
    }
}
