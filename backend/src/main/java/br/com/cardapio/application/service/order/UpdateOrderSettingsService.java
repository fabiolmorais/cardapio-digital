package br.com.cardapio.application.service.order;

import br.com.cardapio.application.port.in.order.UpdateOrderSettingsCommand;
import br.com.cardapio.application.port.in.order.UpdateOrderSettingsUseCase;
import br.com.cardapio.application.port.out.order.OrderSettingsPort;
import br.com.cardapio.domain.order.model.OrderSettings;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

public class UpdateOrderSettingsService implements UpdateOrderSettingsUseCase {

    private final OrderSettingsPort settings;
    private final Clock clock;

    public UpdateOrderSettingsService(OrderSettingsPort settings, Clock clock) {
        this.settings = settings;
        this.clock = clock;
    }

    @Override
    @Transactional
    public OrderSettings update(UpdateOrderSettingsCommand command) {
        Instant now = clock.instant();
        OrderSettings updated = settings.find(command.establishmentId())
                .orElseGet(() -> OrderSettings.defaults(command.establishmentId(), now))
                .update(command.orderingEnabled(), command.approvalRequired(), command.maxAmount(),
                        command.maxItems(), now);
        settings.save(updated);
        return updated;
    }
}
