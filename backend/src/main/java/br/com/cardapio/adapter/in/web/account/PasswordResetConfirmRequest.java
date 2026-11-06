package br.com.cardapio.adapter.in.web.account;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "PasswordResetConfirmRequest", description = "O token do link e a senha nova")
record PasswordResetConfirmRequest(
        @Schema(requiredMode = REQUIRED, description = "O token que veio no link do e-mail") @NotBlank @Size(max = 200)
        String token,
        @Schema(requiredMode = REQUIRED, description = "Pelo menos 10 caracteres, com letra e número")
        @NotBlank @Size(max = 200) String newPassword) {

    @Override
    public String toString() {
        return "PasswordResetConfirmRequest[protegido]";
    }
}
