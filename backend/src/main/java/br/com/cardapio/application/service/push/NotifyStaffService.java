package br.com.cardapio.application.service.push;

import br.com.cardapio.application.port.in.push.NotifyStaffUseCase;
import br.com.cardapio.application.port.out.floor.DiningTablePort;
import br.com.cardapio.application.port.out.push.PushSenderPort.Delivery;
import br.com.cardapio.application.port.out.push.PushSenderPort;
import br.com.cardapio.application.port.out.push.PushSubscriptionPort;
import br.com.cardapio.domain.call.event.CallChanged;
import br.com.cardapio.domain.call.model.CallStatus;
import br.com.cardapio.domain.floor.model.DiningTable;
import br.com.cardapio.domain.order.event.OrderChanged;
import br.com.cardapio.domain.order.model.OrderStatus;
import br.com.cardapio.domain.push.model.PushMessage;
import br.com.cardapio.domain.push.model.PushSubscription;
import br.com.cardapio.domain.shared.event.DomainEvent;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public class NotifyStaffService implements NotifyStaffUseCase {

    /** Quem atende a mesa: dono, gerente e garçom. A cozinha acompanha pela tela dela. */
    static final List<String> FLOOR_ROLES = List.of("OWNER", "MANAGER", "STAFF");
    /** O toque na notificação abre o painel (o app instalado). */
    static final String PANEL = "/painel";

    private final DiningTablePort tables;
    private final PushSubscriptionPort subscriptions;
    private final PushSenderPort sender;

    public NotifyStaffService(DiningTablePort tables, PushSubscriptionPort subscriptions, PushSenderPort sender) {
        this.tables = tables;
        this.subscriptions = subscriptions;
        this.sender = sender;
    }

    /**
     * Só o que pede alguém indo à mesa: o chamado que acabou de chegar (ou que mudou a forma de
     * pagamento) e o pedido parado na confirmação do garçom. A tag é a do chamado ou do pedido: um
     * aviso repetido substitui o anterior no aparelho, em vez de empilhar.
     */
    @Override
    @Transactional
    public void notify(DomainEvent event) {
        if (!sender.enabled()) {
            return;
        }
        Optional<PushMessage> message = switch (event) {
            case CallChanged c when c.status() == CallStatus.PENDING -> label(c).map(table -> new PushMessage(
                    "Mesa " + table, callText(c), PANEL, "call-" + c.callId().value()));
            case OrderChanged o when o.status() == OrderStatus.PENDING_APPROVAL -> label(o).map(table ->
                    new PushMessage("Mesa " + table, "Pedido para confirmar", PANEL, "order-" + o.orderId().value()));
            default -> Optional.empty();
        };
        message.ifPresent(m -> {
            for (PushSubscription subscription : subscriptions.findForRoles(event.establishmentId(), FLOOR_ROLES)) {
                if (sender.send(subscription, m) == Delivery.GONE) {
                    subscriptions.deleteGone(subscription.endpoint());
                }
            }
        });
    }

    private Optional<String> label(CallChanged c) {
        return tables.find(c.establishmentId(), c.tableId()).map(DiningTable::label);
    }

    private Optional<String> label(OrderChanged o) {
        return tables.find(o.establishmentId(), o.tableId()).map(DiningTable::label);
    }

    private static String callText(CallChanged c) {
        return switch (c.type()) {
            case WAITER -> "Chamou o garçom";
            case BILL -> "Pediu a conta";
            case UNLOCK -> "Pede para liberar a mesa";
            case RODIZIO -> "Quer o rodízio";
        };
    }
}
