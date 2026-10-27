package br.com.cardapio.application.port.out.account;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.MembershipId;
import br.com.cardapio.domain.account.model.PinLockout;

import java.util.Optional;

public interface PinLockoutPort {

    /**
     * Trava o vínculo da pessoa até o fim da transação e devolve as tentativas dela (vazio se
     * ela nunca errou). Com a trava, duas tentativas ao mesmo tempo para a mesma pessoa
     * entram uma depois da outra, e a contagem não se perde.
     */
    Optional<PinLockout> lockAndFind(EstablishmentId establishmentId, MembershipId membershipId);

    void save(PinLockout lockout);
}
