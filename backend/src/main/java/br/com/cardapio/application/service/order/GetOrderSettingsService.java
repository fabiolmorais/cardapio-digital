package br.com.cardapio.application.service.order;

import br.com.cardapio.application.port.in.order.GetOrderSettingsUseCase;
import br.com.cardapio.application.port.out.order.OrderSettingsPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.order.model.OrderSettings;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

public class GetOrderSettingsService implements GetOrderSettingsUseCase {

    private final OrderSettingsPort settings;
    private final Clock clock;

    public GetOrderSettingsService(OrderSettingsPort settings, Clock clock) {
        this.settings = settings;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderSettings get(EstablishmentId establishmentId) {
        return settings.find(establishmentId).orElseGet(() -> OrderSettings.defaults(establishmentId, clock.instant()));
    }
}
