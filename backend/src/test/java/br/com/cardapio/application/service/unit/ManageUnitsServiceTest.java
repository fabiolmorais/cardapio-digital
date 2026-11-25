package br.com.cardapio.application.service.unit;

import br.com.cardapio.application.port.in.unit.CreateUnitCommand;
import br.com.cardapio.application.port.in.unit.UnitAllowance;
import br.com.cardapio.application.port.out.account.LoadEstablishmentPort;
import br.com.cardapio.application.port.out.account.UnitPort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.exception.UnitPermissionException;
import br.com.cardapio.domain.account.model.*;
import br.com.cardapio.domain.plan.exception.PlanLimitReachedException;
import br.com.cardapio.domain.plan.model.PlanUsage;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.support.InMemorySubscriptions;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ManageUnitsServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-25T15:00:00Z");

    private final FakeUnits units = new FakeUnits();
    private final InMemorySubscriptions subscriptions = new InMemorySubscriptions()
            .given(InMemorySubscriptions.ENTERPRISE, new PlanUsage(0, 0, 0));
    private final ManageUnitsService service = new ManageUnitsService(units, units, subscriptions,
            Clock.fixed(NOW, ZoneOffset.UTC));

    private final EstablishmentId pinheiros = EstablishmentId.newId();
    private final UserId marina = UserId.newId();

    private CreateUnitCommand create(String name, Role role) {
        return new CreateUnitCommand(pinheiros, marina, role, name, "America/Sao_Paulo");
    }

    @Test
    void theOwnerCreatesAUnitInTheSameCompanyAndBecomesItsOwner() {
        UnitSummary created = service.create(create("Sakura Moema", Role.OWNER));

        assertThat(created.name()).isEqualTo("Sakura Moema");
        assertThat(created.createdAt()).isEqualTo(NOW);
        assertThat(units.saved).singleElement().satisfies(unit -> {
            assertThat(unit.organizationId()).isEqualTo(units.company);
            assertThat(unit.slug().value()).isEqualTo("sakura-moema");
        });
        assertThat(units.owners).singleElement().satisfies(owner -> {
            assertThat(owner.userId()).isEqualTo(marina);
            assertThat(owner.role()).isEqualTo(Role.OWNER);
            assertThat(owner.establishmentId()).isEqualTo(created.id());
        });
        assertThat(subscriptions.locks).isEqualTo(1);
    }

    @Test
    void aTakenAddressGetsASuffix() {
        units.taken.add("sakura-moema");
        assertThat(service.create(create("Sakura Moema", Role.OWNER)).slug().value()).isEqualTo("sakura-moema-2");
    }

    @Test
    void onlyTheOwnerAndOnlyWithRoomInThePlan() {
        assertThatThrownBy(() -> service.create(create("Sakura Moema", Role.MANAGER)))
                .isInstanceOf(UnitPermissionException.class);
        assertThatThrownBy(() -> service.create(create(" ", Role.OWNER)))
                .isInstanceOf(InvalidValueException.class).hasMessage("Informe o nome da unidade");

        subscriptions.given(InMemorySubscriptions.PRO, new PlanUsage(0, 0, 0));
        assertThatThrownBy(() -> service.create(create("Sakura Moema", Role.OWNER)))
                .isInstanceOf(PlanLimitReachedException.class);
        assertThat(units.saved).isEmpty();
    }

    @Test
    void theAllowanceComesFromThePlan() {
        units.active = 3;
        UnitAllowance enterprise = service.allowance(pinheiros);
        assertThat(enterprise.max()).isNull();
        assertThat(enterprise.canAddMore()).isTrue();

        subscriptions.given(InMemorySubscriptions.PRO, new PlanUsage(0, 0, 0));
        units.active = 1;
        UnitAllowance pro = service.allowance(pinheiros);
        assertThat(pro.max()).isEqualTo(1);
        assertThat(pro.canAddMore()).isFalse();
    }

    private static final class FakeUnits implements UnitPort, LoadEstablishmentPort {
        final OrganizationId company = OrganizationId.newId();
        final List<Establishment> saved = new ArrayList<>();
        final List<Membership> owners = new ArrayList<>();
        final Set<String> taken = new HashSet<>();
        long active = 1;

        @Override
        public OrganizationId organizationOf(EstablishmentId establishmentId) {
            return company;
        }

        @Override
        public long countActive(OrganizationId organizationId) {
            return active + saved.size();
        }

        @Override
        public ResultPage<UnitSummary> list(OrganizationId organizationId, PageQuery page) {
            return new ResultPage<>(List.of(), page.page(), page.size(), 0);
        }

        @Override
        public List<UnitMembership> unitsFor(OrganizationId organizationId, UserId userId) {
            return List.of();
        }

        @Override
        public void saveUnit(Establishment unit, Membership owner) {
            saved.add(unit);
            owners.add(owner);
        }

        @Override
        public boolean existsBySlug(Slug slug) {
            return taken.contains(slug.value());
        }
    }
}
