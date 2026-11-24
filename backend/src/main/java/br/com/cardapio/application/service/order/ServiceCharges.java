package br.com.cardapio.application.service.order;

import br.com.cardapio.application.port.out.order.OrderPort;
import br.com.cardapio.application.port.out.order.OrderSettingsPort;
import br.com.cardapio.application.port.out.order.TabPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.order.model.OrderSettings;
import br.com.cardapio.domain.order.model.ServiceChargeSettings;
import br.com.cardapio.domain.order.model.ServiceChoice;
import br.com.cardapio.domain.order.model.ServiceDestination;
import br.com.cardapio.domain.order.model.Tab;

import java.util.Optional;
import java.util.Set;

/** O que os casos de uso da conta repetem sobre a taxa de serviço (RF39). Pública: a usam outros pacotes. */
public final class ServiceCharges {

    private ServiceCharges() {
    }

    /** A configuração do restaurante (desligada, se ele nunca mexeu). */
    public static ServiceChargeSettings settingsOf(OrderSettingsPort settings, EstablishmentId establishmentId) {
        return settings.find(establishmentId).map(OrderSettings::serviceCharge).orElse(ServiceChargeSettings.DEFAULT);
    }

    /** O garçom que fica com a taxa: o que mais atendeu, se a regra for "garçom que atendeu". */
    public static Optional<UserId> waiterOf(TabPort tabs, ServiceChargeSettings settings, Tab tab) {
        return settings.destination() == ServiceDestination.WAITER ? tabs.findTopAttendant(tab) : Optional.empty();
    }

    /** O nome do garçom que fica com a taxa, para a tela; null na caixinha da equipe. */
    public static String waiterNameOf(TabPort tabs, OrderPort orders, ServiceChargeSettings settings, Tab tab) {
        return waiterOf(tabs, settings, tab)
                .map(id -> orders.findStaffNames(tab.establishmentId(), Set.of(id)).get(id))
                .orElse(null);
    }

    /**
     * Aplica a escolha do garçom (o percentual, sem taxa ou outro valor) na comanda e grava. Null: fica
     * como está.
     * Devolve a comanda como ficou.
     */
    public static Tab choose(TabPort tabs, ServiceChargeSettings settings, Tab tab, ServiceChoice choice) {
        if (choice == null) {
            return tab;
        }
        Tab chosen = tab.chooseService(settings, choice);
        if (!chosen.equals(tab)) {
            tabs.saveService(chosen);
        }
        return chosen;
    }
}
