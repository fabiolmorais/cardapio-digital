package br.com.cardapio.application.port.in.account;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.Role;

/** Estabelecimento em que a pessoa está trabalhando, com o papel dela ali. */
public record EstablishmentAccess(
        EstablishmentId establishmentId,
        String name,
        String slug,
        Role role
) {
}
