package br.com.cardapio.application.port.in.team;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.MembershipId;
import br.com.cardapio.domain.account.model.Role;

import java.util.Objects;

public record DeactivateTeamMemberCommand(
        EstablishmentId establishmentId,
        Role actorRole,
        MembershipId memberId
) {

    public DeactivateTeamMemberCommand {
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(actorRole);
        Objects.requireNonNull(memberId);
    }
}
