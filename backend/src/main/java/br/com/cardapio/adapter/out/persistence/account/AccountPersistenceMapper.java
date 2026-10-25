package br.com.cardapio.adapter.out.persistence.account;

import br.com.cardapio.domain.account.model.Establishment;
import br.com.cardapio.domain.account.model.Membership;
import br.com.cardapio.domain.account.model.Organization;
import br.com.cardapio.domain.account.model.User;

final class AccountPersistenceMapper {

    public AccountPersistenceMapper() {
    }

    static OrganizationJpaEntity toEntity(Organization o) {
        return new OrganizationJpaEntity(o.id().value(), o.name(), o.document().value(), o.whatsapp().value(),
                o.email().value());
    }

    static EstablishmentJpaEntity toEntity(Establishment e) {
        return new EstablishmentJpaEntity(e.id().value(), e.organizationId().value(), e.name(), e.slug().value(),
                e.timeZone(), e.active());
    }

    static UserJpaEntity toEntity(User u) {
        return new UserJpaEntity(u.id().value(), u.name(), u.email().value(), u.passwordHash().value(), u.active());
    }

    static MembershipJpaEntity toEntity(Membership m) {
        return new MembershipJpaEntity(m.id().value(), m.userId().value(), m.establishmentId().value(),
                m.role().name(), m.active());
    }
}
