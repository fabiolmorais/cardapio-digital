package br.com.cardapio.adapter.in.web.team;

import br.com.cardapio.adapter.in.web.shared.AuthenticatedUser;
import br.com.cardapio.application.port.in.team.UpdateTeamMemberCommand;
import br.com.cardapio.domain.account.model.MembershipId;
import br.com.cardapio.domain.account.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

@Schema(name = "UpdateTeamMemberRequest", description = "Só os campos enviados mudam")
record UpdateTeamMemberRequest(
        @Schema(description = "Novo papel: MANAGER, STAFF ou KITCHEN") Role role,
        @Schema(description = "Novo PIN: 4 a 6 números", example = "7391") @Pattern(regexp = "\\d{4,6}") String pin
) {
    UpdateTeamMemberCommand toCommand(AuthenticatedUser user, MembershipId memberId) {
        return new UpdateTeamMemberCommand(user.establishmentId(), user.role(), memberId, role, pin);
    }

    @Override
    public String toString() {
        return "UpdateTeamMemberRequest[role=" + role + "]";
    }
}
