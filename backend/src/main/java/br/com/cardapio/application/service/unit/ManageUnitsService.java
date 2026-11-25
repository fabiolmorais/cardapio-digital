package br.com.cardapio.application.service.unit;

import br.com.cardapio.application.port.in.unit.CreateUnitCommand;
import br.com.cardapio.application.port.in.unit.ManageUnitsUseCase;
import br.com.cardapio.application.port.in.unit.UnitAllowance;
import br.com.cardapio.application.port.out.account.LoadEstablishmentPort;
import br.com.cardapio.application.port.out.account.UnitPort;
import br.com.cardapio.application.port.out.plan.CurrentSubscription;
import br.com.cardapio.application.port.out.plan.SubscriptionPort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.application.service.account.Slugs;
import br.com.cardapio.domain.account.exception.UnitPermissionException;
import br.com.cardapio.domain.account.model.Establishment;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.Membership;
import br.com.cardapio.domain.account.model.OrganizationId;
import br.com.cardapio.domain.account.model.Role;
import br.com.cardapio.domain.account.model.UnitSummary;
import br.com.cardapio.domain.plan.model.PlanResource;
import br.com.cardapio.domain.plan.service.PlanAllowance;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

public class ManageUnitsService implements ManageUnitsUseCase {

    private final UnitPort units;
    private final LoadEstablishmentPort establishments;
    private final SubscriptionPort subscriptions;
    private final Clock clock;

    public ManageUnitsService(UnitPort units, LoadEstablishmentPort establishments, SubscriptionPort subscriptions,
                              Clock clock) {
        this.units = units;
        this.establishments = establishments;
        this.subscriptions = subscriptions;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPage<UnitSummary> list(EstablishmentId current, PageQuery page) {
        return units.list(units.organizationOf(current), page);
    }

    @Override
    @Transactional(readOnly = true)
    public UnitAllowance allowance(EstablishmentId current) {
        CurrentSubscription subscription = require(subscriptions.find(current));
        long used = units.countActive(units.organizationOf(current));
        return new UnitAllowance(subscription.plan().name(), used, subscription.plan().maxUnits().max());
    }

    @Override
    @Transactional
    public UnitSummary create(CreateUnitCommand command) {
        if (command.actorRole() != Role.OWNER) {
            throw new UnitPermissionException();
        }
        // Trava a assinatura: duas unidades criadas ao mesmo tempo não passam juntas da última vaga.
        CurrentSubscription subscription = require(subscriptions.lock(command.current()));
        OrganizationId organization = units.organizationOf(command.current());
        PlanAllowance.checkRoomForOneMore(subscription.plan(), units.countActive(organization), PlanResource.UNITS);

        // O nome é conferido antes do endereço, para o erro falar do nome.
        Establishment unit = Establishment.unit(organization, command.name(),
                Slugs.available(establishments, requireName(command.name())), command.timeZone());
        units.saveUnit(unit, Membership.owner(command.userId(), unit.id()));
        return new UnitSummary(unit.id(), unit.name(), unit.slug(), clock.instant(), 0, 1);
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidValueException("Informe o nome da unidade");
        }
        return name.strip();
    }

    private static CurrentSubscription require(Optional<CurrentSubscription> current) {
        return current.orElseThrow(() -> new IllegalStateException("restaurante sem assinatura"));
    }
}
