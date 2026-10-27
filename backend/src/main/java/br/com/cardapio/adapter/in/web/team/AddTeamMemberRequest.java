package br.com.cardapio.adapter.in.web.team;

import br.com.cardapio.adapter.in.web.shared.AuthenticatedUser;
import br.com.cardapio.application.port.in.team.AddTeamMemberCommand;
import br.com.cardapio.domain.account.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "AddTeamMemberRequest", description = "Cadastro de uma pessoa da equipe")
record AddTeamMemberRequest(
        @Schema(requiredMode = REQUIRED, example = "Carlos Souza") @NotBlank @Size(max = 120) String name,
        @Schema(requiredMode = REQUIRED, description = "MANAGER, STAFF ou KITCHEN") @NotNull Role role,
        @Schema(requiredMode = REQUIRED, description = "6 números, sem sequências óbvias", example = "258014")
        @NotBlank @Pattern(regexp = "\\d{6}") String pin
) {

    AddTeamMemberCommand toCommand(AuthenticatedUser user) {
        return new AddTeamMemberCommand(user.establishmentId(), user.role(), name, role, pin);
    }

    /** O PIN nunca aparece em log. */
    @Override
    public String toString() {
        return "AddTeamMemberRequest[role=" + role + "]";
    }
}
