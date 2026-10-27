package br.com.cardapio.application.port.in.team;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.Role;

import java.util.Objects;

/**
 * @param establishmentId restaurante de quem está cadastrando (vem do token, nunca do corpo)
 * @param actorRole       papel de quem está cadastrando (vem do token)
 */
public record AddTeamMemberCommand(
        EstablishmentId establishmentId,
        Role actorRole,
        String name,
        Role role,
        String pin
) {
    public AddTeamMemberCommand {
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(actorRole);
        Objects.requireNonNull(role);
    }

    /** O PIN nunca aparece em log, nem por engano. */
    @Override
    public String toString() {
        return "AddTeamMemberCommand[establishmentId=" + establishmentId + ", role=" + role + "]";
    }
}
