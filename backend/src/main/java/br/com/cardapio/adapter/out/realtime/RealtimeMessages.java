package br.com.cardapio.adapter.out.realtime;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.call.event.CallChanged;
import br.com.cardapio.domain.call.model.CallId;
import br.com.cardapio.domain.call.model.CallStatus;
import br.com.cardapio.domain.call.model.CallType;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.order.event.OrderChanged;
import br.com.cardapio.domain.order.event.TabClosed;
import br.com.cardapio.domain.order.model.OrderId;
import br.com.cardapio.domain.order.model.OrderStatus;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.rodizio.event.RodizioChanged;
import br.com.cardapio.domain.shared.event.DomainEvent;
import br.com.cardapio.domain.tablesession.event.PhoneBlockChanged;
import br.com.cardapio.domain.tablesession.model.TableSessionId;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;

/**
 * O evento como texto, para viajar pelo NOTIFY do Postgres entre as instâncias do back. Cada
 * tipo de evento tem a sua mensagem, com um "kind" que diz qual é. O limite do NOTIFY é 8000
 * bytes; as mensagens têm só ids e status, bem abaixo disso.
 */
@Component
class RealtimeMessages {

    /** O canal do LISTEN/NOTIFY. Um só para todos os restaurantes: quem filtra é o adaptador. */
    static final String CHANNEL = "realtime_events";

    private final JsonMapper json;

    RealtimeMessages(JsonMapper json) {
        this.json = json;
    }

    record CallChangedMessage(String kind, UUID establishmentId, UUID tableId, UUID callId, String type,
                              String status, Instant occurredAt) {
        static final String KIND = "CallChanged";
    }

    record OrderChangedMessage(String kind, UUID establishmentId, UUID tableId, UUID orderId, String status,
                               Instant occurredAt) {
        static final String KIND = "OrderChanged";
    }

    record TabClosedMessage(String kind, UUID establishmentId, UUID tableId, UUID tabId, Instant occurredAt) {
        static final String KIND = "TabClosed";
    }

    record RodizioChangedMessage(String kind, UUID establishmentId, UUID tableId, UUID tabId, Instant occurredAt) {
        static final String KIND = "RodizioChanged";
    }

    record PhoneBlockChangedMessage(String kind, UUID establishmentId, UUID tableId, UUID sessionId, boolean blocked,
                                    Instant occurredAt) {
        static final String KIND = "PhoneBlockChanged";
    }

    String toJson(DomainEvent event) {
        Object message = switch (event) {
            case OrderChanged o -> new OrderChangedMessage(OrderChangedMessage.KIND, o.establishmentId().value(),
                    o.tableId().value(), o.orderId().value(), o.status().name(), o.occurredAt());
            case CallChanged c -> new CallChangedMessage(CallChangedMessage.KIND, c.establishmentId().value(),
                    c.tableId().value(), c.callId().value(), c.type().name(), c.status().name(), c.occurredAt());
            case TabClosed t -> new TabClosedMessage(TabClosedMessage.KIND, t.establishmentId().value(),
                    t.tableId().value(), t.tabId().value(), t.occurredAt());
            case RodizioChanged r -> new RodizioChangedMessage(RodizioChangedMessage.KIND, r.establishmentId().value(),
                    r.tableId().value(), r.tabId().value(), r.occurredAt());
            case PhoneBlockChanged p -> new PhoneBlockChangedMessage(PhoneBlockChangedMessage.KIND,
                    p.establishmentId().value(), p.tableId().value(), p.sessionId().value(), p.blocked(),
                    p.occurredAt());
            default -> throw new IllegalArgumentException("Evento sem mensagem de tempo real: " + event.getClass());
        };
        return json.writeValueAsString(message);
    }

    /** O evento de volta. Um "kind" desconhecido (de uma versão mais nova do back) vira IllegalArgumentException. */
    DomainEvent fromJson(String text) {
        JsonNode node = json.readTree(text);
        String kind = node.path("kind").asString();
        if (CallChangedMessage.KIND.equals(kind)) {
            CallChangedMessage m = json.treeToValue(node, CallChangedMessage.class);
            return new CallChanged(new EstablishmentId(m.establishmentId()), new DiningTableId(m.tableId()),
                    new CallId(m.callId()), CallType.valueOf(m.type()), CallStatus.valueOf(m.status()),
                    m.occurredAt());
        }
        if (OrderChangedMessage.KIND.equals(kind)) {
            OrderChangedMessage m = json.treeToValue(node, OrderChangedMessage.class);
            return new OrderChanged(new EstablishmentId(m.establishmentId()), new DiningTableId(m.tableId()),
                    new OrderId(m.orderId()), OrderStatus.valueOf(m.status()), m.occurredAt());
        }
        if (TabClosedMessage.KIND.equals(kind)) {
            TabClosedMessage m = json.treeToValue(node, TabClosedMessage.class);
            return new TabClosed(new EstablishmentId(m.establishmentId()), new DiningTableId(m.tableId()),
                    new TabId(m.tabId()), m.occurredAt());
        }
        if (RodizioChangedMessage.KIND.equals(kind)) {
            RodizioChangedMessage m = json.treeToValue(node, RodizioChangedMessage.class);
            return new RodizioChanged(new EstablishmentId(m.establishmentId()), new DiningTableId(m.tableId()),
                    new TabId(m.tabId()), m.occurredAt());
        }
        if (PhoneBlockChangedMessage.KIND.equals(kind)) {
            PhoneBlockChangedMessage m = json.treeToValue(node, PhoneBlockChangedMessage.class);
            return new PhoneBlockChanged(new EstablishmentId(m.establishmentId()), new DiningTableId(m.tableId()),
                    new TableSessionId(m.sessionId()), m.blocked(), m.occurredAt());
        }
        throw new IllegalArgumentException("Mensagem de tempo real desconhecida: " + kind);
    }
}
