package br.com.cardapio.application.port.out.order;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.order.model.OrderSettings;

import java.util.Optional;

public interface OrderSettingsPort {

    /** Vazio se o restaurante nunca mexeu: valem os padrões (OrderSettings.defaults). */
    Optional<OrderSettings> find(EstablishmentId establishmentId);

    void save(OrderSettings settings);
}
