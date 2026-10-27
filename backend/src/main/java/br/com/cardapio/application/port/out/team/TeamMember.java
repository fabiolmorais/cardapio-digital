package br.com.cardapio.application.port.out.team;

import br.com.cardapio.domain.account.model.Membership;

/** O vínculo de uma pessoa com o restaurante, junto com o nome dela (que fica em User). */
public record TeamMember(Membership membership, String name) {
}
