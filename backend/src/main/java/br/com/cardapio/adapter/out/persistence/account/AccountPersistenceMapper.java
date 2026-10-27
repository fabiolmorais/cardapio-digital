package br.com.cardapio.adapter.out.persistence.account;

import br.com.cardapio.application.port.out.account.ActiveMembership;
import br.com.cardapio.application.port.out.team.TeamMember;
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
        return new UserJpaEntity(u.id().value(), u.name(),
                u.email() == null ? null : u.email().value(),
                u.passwordHash() == null ? null : u.passwordHash().value(),
                u.active());
    }

    static MembershipJpaEntity toEntity(Membership m) {
        return new MembershipJpaEntity(m.id().value(), m.userId().value(), m.establishmentId().value(),
                m.role().name(), m.pinHash() == null ? null : m.pinHash().value(), m.active());
    }

    static User toDomain(UserJpaEntity e) {
        return new User(new UserId(e.getId()), e.getName(),
                e.getEmail() == null ? null : new Email(e.getEmail()),
                e.getPasswordHash() == null ? null : new PasswordHash(e.getPasswordHash()),
                e.isActive());
    }

    static TeamMember toDomain(TeamMemberRow row) {
        Membership membership = new Membership(new MembershipId(row.getId()), new UserId(row.getUserId()),
                new EstablishmentId(row.getEstablishmentId()), Role.valueOf(row.getRole()),
                row.getPinHash() == null ? null : new PasswordHash(row.getPinHash()), row.getActive());
        return new TeamMember(membership, row.getName());
    }

    static ActiveMembership toDomain(ActiveMembershipRow row) {
        return new ActiveMembership(new MembershipId(row.getMembershipId()),
                new EstablishmentId(row.getEstablishmentId()), row.getEstablishmentName(),
                row.getEstablishmentSlug(), Role.valueOf(row.getRole()));
    }

    static RefreshTokenJpaEntity toEntity(RefreshToken t) {
        return new RefreshTokenJpaEntity(t.id().value(), t.familyId().value(), t.userId().value(),
                t.establishmentId().value(), t.hash().value(), t.createdAt(), t.expiresAt(), t.revokedAt());
    }

    static RefreshToken toDomain(RefreshTokenJpaEntity e) {
        return new RefreshToken(new RefreshTokenId(e.getId()), new RefreshTokenId(e.getFamilyId()),
                new UserId(e.getUserId()), new EstablishmentId(e.getEstablishmentId()),
                new RefreshTokenHash(e.getTokenHash()), e.getCreatedAt(), e.getExpiresAt(), e.getRevokedAt());
    }
}
