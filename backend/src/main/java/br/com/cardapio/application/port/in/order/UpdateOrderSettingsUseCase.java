package br.com.cardapio.application.port.in.order;

import br.com.cardapio.domain.order.model.OrderSettings;

public interface UpdateOrderSettingsUseCase {

    OrderSettings update(UpdateOrderSettingsCommand command);
}
