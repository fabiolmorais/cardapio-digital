package br.com.cardapio.application.port.out.security;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.Role;
import br.com.cardapio.domain.account.model.UserId;

/** Quem é a pessoa, em qual estabelecimento está trabalhando e com qual papel. */
public record TokenSubject(
        UserId userId,
        EstablishmentId establishmentId,
        Role role
) {
}
