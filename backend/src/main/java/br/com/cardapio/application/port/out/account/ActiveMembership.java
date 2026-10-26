package br.com.cardapio.application.port.out.account;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.MembershipId;
import br.com.cardapio.domain.account.model.Role;

/**
 * Vínculo ativo de uma pessoa com um estabelecimento ativo, já com os dados do estabelecimento
 * que o login e o "quem sou eu" precisam mostrar.
 */
public record ActiveMembership(
        MembershipId membershipId,
        EstablishmentId establishmentId,
        String establishmentName,
        String establishmentSlug,
        Role role
) {
}
