package br.com.cardapio.adapter.in.web.account;

import br.com.cardapio.application.port.in.account.SignupCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "SignupRequest", description = "Cadastro self-service do restaurante")
record SignupRequest(
        @Schema(requiredMode = REQUIRED, example = "Sakura Sushi") @NotBlank @Size(max = 120) String establishmentName,
        @Schema(requiredMode = REQUIRED, example = "Marina Tanaka") @NotBlank @Size(max = 120) String ownerName,
        @Schema(requiredMode = REQUIRED, description = "CPF ou CNPJ, com ou sem pontuação", example = "11.222.333/0001-81")
        @NotBlank @Size(max = 18) String document,
        @Schema(requiredMode = REQUIRED, example = "(11) 98765-4321") @NotBlank @Size(max = 20) String whatsapp,
        @Schema(requiredMode = REQUIRED, example = "marina@sakurasushi.com.br") @NotBlank @Email @Size(max = 254) String email,
        @Schema(requiredMode = REQUIRED, description = "Mínimo de 10 caracteres, com letras e números")
        @NotBlank @Size(min = 10, max = 72) String password,
        @Schema(requiredMode = REQUIRED, description = "Aceite dos termos de uso e da política de privacidade")
        @NotNull @AssertTrue Boolean acceptedTerms
) {

    SignupCommand toCommand() {
        return new SignupCommand(establishmentName, ownerName, document, whatsapp, email, password);
    }

    @Override
    public String toString() {
        return "SignupRequest[establishmentName=" + establishmentName + "]";
    }
}
