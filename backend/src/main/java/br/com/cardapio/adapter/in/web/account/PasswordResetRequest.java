package br.com.cardapio.adapter.in.web.account;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "PasswordResetRequest", description = "O e-mail de quem esqueceu a senha")
record PasswordResetRequest(
        @Schema(requiredMode = REQUIRED, example = "marina@sakura.com") @NotBlank @Size(max = 254) String email) {

    @Override
    public String toString() {
        return "PasswordResetRequest[protegido]";
    }
}
