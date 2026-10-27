package br.com.cardapio.adapter.in.web.account;

import br.com.cardapio.application.port.in.account.PinLoginCommand;
import br.com.cardapio.domain.account.model.MembershipId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "PinLoginRequest", description = "Quem tocou no próprio nome e o PIN digitado")
record PinLoginRequest(
        @Schema(requiredMode = REQUIRED) @NotNull UUID memberId,
        @Schema(requiredMode = REQUIRED, example = "258014") @NotBlank @Pattern(regexp = "\\d{6}") String pin
) {

    PinLoginCommand toCommand(String deviceToken) {
        return new PinLoginCommand(deviceToken, new MembershipId(memberId), pin);
    }

    @Override
    public String toString() {
        return "PinLoginRequest[memberId=" + memberId + "]";
    }
}
