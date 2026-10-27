package br.com.cardapio.adapter.in.web.team;

import br.com.cardapio.application.port.in.team.TeamMemberSummary;
import br.com.cardapio.domain.account.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "TeamMember", description = "Pessoa da equipe do restaurante")
record TeamMemberResponse(
        @Schema(requiredMode = REQUIRED) UUID id,
        @Schema(requiredMode = REQUIRED, example = "Carlos Souza") String name,
        @Schema(requiredMode = REQUIRED) Role role,
        @Schema(requiredMode = REQUIRED, description = "false = desligado do restaurante") boolean active
) {

    static TeamMemberResponse from(TeamMemberSummary s) {
        return new TeamMemberResponse(s.id().value(), s.name(), s.role(), s.active());
    }
}
