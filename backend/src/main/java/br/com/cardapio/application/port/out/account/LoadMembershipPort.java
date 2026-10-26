package br.com.cardapio.application.port.out.account;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;

import java.util.List;
import java.util.Optional;

public interface LoadMembershipPort {

    /** Todos os estabelecimentos em que a pessoa trabalha (login: ela ainda não escolheu um). */
    List<ActiveMembership> findActiveByUser(UserId userId);

    /** O vínculo da pessoa com um estabelecimento específico; vazio se ela não trabalha lá. */
    Optional<ActiveMembership> findActive(UserId userId, EstablishmentId establishmentId);
}
