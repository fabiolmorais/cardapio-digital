package br.com.cardapio.adapter.out.persistence.order;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentFilter;
import br.com.cardapio.application.port.out.order.OrderSettingsPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.order.model.OrderSettings;
import br.com.cardapio.domain.order.model.OrderSettingsId;
import br.com.cardapio.domain.shared.model.Money;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class OrderSettingsPersistenceAdapter implements OrderSettingsPort {

    private final OrderSettingsJpaRepository settings;
    private final EstablishmentFilter establishmentFilter;

    OrderSettingsPersistenceAdapter(OrderSettingsJpaRepository settings, EstablishmentFilter establishmentFilter) {
        this.settings = settings;
        this.establishmentFilter = establishmentFilter;
    }

    @Override
    public Optional<OrderSettings> find(EstablishmentId establishmentId) {
        establishmentFilter.enable(establishmentId);
        return settings.findByEstablishmentId(establishmentId.value()).map(e -> new OrderSettings(
                new OrderSettingsId(e.getId()), new EstablishmentId(e.getEstablishmentId()), e.isOrderingEnabled(),
                e.isApprovalRequired(), e.getMaxAmountCents() == null ? null : Money.ofCents(e.getMaxAmountCents()),
                e.getMaxItems(), e.getUpdatedAt()));
    }

    @Override
    public void save(OrderSettings s) {
        establishmentFilter.enable(s.establishmentId());
        settings.save(new OrderSettingsJpaEntity(s.id().value(), s.establishmentId().value(), s.orderingEnabled(),
                s.approvalRequired(), s.maxAmount() == null ? null : s.maxAmount().cents(), s.maxItems(),
                s.updatedAt()));
    }
}
