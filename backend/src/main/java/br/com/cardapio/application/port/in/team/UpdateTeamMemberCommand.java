package br.com.cardapio.application.port.in.team;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.MembershipId;
import br.com.cardapio.domain.account.model.Role;

import java.util.Objects;

/** newRole e newPin são opcionais: null quer dizer "não mudar". */
public record UpdateTeamMemberCommand(
        EstablishmentId establishmentId,
        Role actorRole,
        MembershipId memberId,
        Role newRole,
        String newPin
) {

    public UpdateTeamMemberCommand {
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(actorRole);
        Objects.requireNonNull(memberId);
    }

    @Override
    public String toString() {
        return "UpdateTeamMemberCommand[memberId=" + memberId + ", newRole=" + newRole + "]";
    }
}
