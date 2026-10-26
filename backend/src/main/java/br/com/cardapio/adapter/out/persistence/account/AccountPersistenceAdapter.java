package br.com.cardapio.adapter.out.persistence.account;

import br.com.cardapio.application.port.out.account.*;
import br.com.cardapio.domain.account.model.*;
import br.com.cardapio.domain.shared.model.Email;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
class AccountPersistenceAdapter implements LoadUserPort, LoadEstablishmentPort, LoadMembershipPort, SaveAccountPort {

    private final OrganizationJpaRepository organizations;
    private final EstablishmentJpaRepository establishments;
    private final UserJpaRepository users;
    private final MembershipJpaRepository memberships;

    AccountPersistenceAdapter(OrganizationJpaRepository organizations, EstablishmentJpaRepository establishments, UserJpaRepository users, MembershipJpaRepository memberships) {
        this.organizations = organizations;
        this.establishments = establishments;
        this.users = users;
        this.memberships = memberships;
    }

    @Override
    public boolean existsByEmail(Email email) {
        return users.existsByEmail(email.value());
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return users.findByEmail(email.value()).map(AccountPersistenceMapper::toDomain);
    }

    @Override
    public Optional<User> findById(UserId id) {
        return users.findById(id.value()).map(AccountPersistenceMapper::toDomain);
    }

    @Override
    public List<ActiveMembership> findActiveByUser(UserId userId) {
        return memberships.findActiveByUser(userId.value()).stream()
                .map(AccountPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<ActiveMembership> findActive(UserId userId, EstablishmentId establishmentId) {
        return memberships.findActive(userId.value(), establishmentId.value()).map(AccountPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsBySlug(Slug slug) {
        return establishments.existsBySlug(slug.value());
    }

    @Override
    public void saveNewAccount(Organization organization, Establishment establishment, User owner, Membership membership) {
        organizations.save(AccountPersistenceMapper.toEntity(organization));
        establishments.save(AccountPersistenceMapper.toEntity(establishment));
        users.save(AccountPersistenceMapper.toEntity(owner));
        memberships.save(AccountPersistenceMapper.toEntity(membership));
    }
}
