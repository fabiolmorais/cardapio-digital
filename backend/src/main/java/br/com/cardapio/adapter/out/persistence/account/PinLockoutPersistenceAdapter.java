package br.com.cardapio.adapter.out.persistence.account;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentFilter;
import br.com.cardapio.application.port.out.account.PinLockoutPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.MembershipId;
import br.com.cardapio.domain.account.model.PinLockout;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class PinLockoutPersistenceAdapter implements PinLockoutPort {

    private final MembershipJpaRepository memberships;
    private final PinLockoutJpaRepository lockouts;
    private final EstablishmentFilter establishmentFilter;

    PinLockoutPersistenceAdapter(MembershipJpaRepository memberships, PinLockoutJpaRepository lockouts,
                                 EstablishmentFilter establishmentFilter) {
        this.memberships = memberships;
        this.lockouts = lockouts;
        this.establishmentFilter = establishmentFilter;
    }

    /**
     * A trava fica no vínculo (que sempre existe), e não na linha de tentativas, que só nasce
     * no primeiro erro: não dá para travar uma linha que ainda não existe.
     */
    @Override
    public Optional<PinLockout> lockAndFind(EstablishmentId establishmentId, MembershipId membershipId) {
        establishmentFilter.enable(establishmentId);
        if (memberships.lockById(membershipId.value(), establishmentId.value()).isEmpty()) {
            return Optional.empty();
        }
        return lockouts.findById(membershipId.value()).map(AccountPersistenceMapper::toDomain);
    }

    @Override
    public void save(PinLockout lockout) {
        establishmentFilter.enable(lockout.establishmentId());
        lockouts.save(AccountPersistenceMapper.toEntity(lockout));
    }
}
