package br.com.cardapio.application.port.out.team;

import br.com.cardapio.domain.account.model.Membership;

import java.time.Instant;

/**
 * O vínculo de uma pessoa com o restaurante, junto com o nome dela (que fica em User).
 *
 * @param lastAccessAt a última vez que a pessoa entrou ou renovou a sessão neste restaurante; null se
 *                     nunca entrou (ou se só a lista da equipe é que sabe: as outras consultas não trazem)
 */
public record TeamMember(Membership membership, String name, Instant lastAccessAt) {

    public TeamMember(Membership membership, String name) {
        this(membership, name, null);
    }
}
