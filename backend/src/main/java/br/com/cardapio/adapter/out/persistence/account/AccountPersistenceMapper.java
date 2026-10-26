package br.com.cardapio.adapter.out.persistence.account;

import br.com.cardapio.application.port.out.account.ActiveMembership;
import br.com.cardapio.domain.account.model.*;
import br.com.cardapio.domain.shared.model.Email;

final class AccountPersistenceMapper {

    private AccountPersistenceMapper() {
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

    static User toDomain(UserJpaEntity e) {
        return new User(new UserId(e.getId()), e.getName(), new Email(e.getEmail()),
                new PasswordHash(e.getPasswordHash()), e.isActive());
    }

    static ActiveMembership toDomain(ActiveMembershipRow row) {
        return new ActiveMembership(new MembershipId(row.getMembershipId()),
                new EstablishmentId(row.getEstablishmentId()), row.getEstablishmentName(),
                row.getEstablishmentSlug(), Role.valueOf(row.getRole()));
    }
}
