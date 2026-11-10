package br.com.cardapio.application.port.in.team;

import br.com.cardapio.domain.account.model.MembershipId;
import br.com.cardapio.domain.account.model.Role;

import java.time.Instant;

/** O que a tela da equipe mostra de cada pessoa. O PIN (nem o hash) nunca sai daqui. */
public record TeamMemberSummary(
        MembershipId id,
        String name,
        Role role,
        boolean active,
        /** A última entrada ou renovação da sessão; null se não há registro. */
        Instant lastAccessAt
) {
}
