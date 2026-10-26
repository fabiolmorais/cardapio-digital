package br.com.cardapio.adapter.in.web.account;

import br.com.cardapio.application.port.in.account.LoginCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "LoginRequest")
record LoginRequest(
        @Schema(requiredMode = REQUIRED, example = "marina@sakurasushi.com.br")  @NotBlank @Size(max = 254) String email,
        @Schema(requiredMode = REQUIRED) @NotBlank @Size(max = 72) String password
) {

    LoginCommand toCommand() {
        return new LoginCommand(email, password);
    }

    @Override
    public String toString() {
        return "LoginRequest[protegido]";
    }
}
