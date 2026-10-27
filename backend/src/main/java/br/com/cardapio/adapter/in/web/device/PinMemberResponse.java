package br.com.cardapio.adapter.in.web.device;

import br.com.cardapio.application.port.in.team.TeamMemberSummary;
import br.com.cardapio.domain.account.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "PinMember", description = "Pessoa que pode entrar por PIN neste aparelho")
record PinMemberResponse(
        @Schema(requiredMode = REQUIRED) UUID id,
        @Schema(requiredMode = REQUIRED, example = "Carlos Souza") String name,
        @Schema(requiredMode = REQUIRED) Role role
) {
    static PinMemberResponse from(TeamMemberSummary member) {
        return new PinMemberResponse(member.id().value(), member.name(), member.role());
    }
}
