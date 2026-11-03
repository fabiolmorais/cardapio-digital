package br.com.cardapio.application.port.in.realtime;

import br.com.cardapio.domain.call.event.CallChanged;
import br.com.cardapio.domain.order.event.OrderChanged;
import br.com.cardapio.domain.shared.event.DomainEvent;

import java.util.Map;
import java.util.Optional;

/**
 * O aviso como a tela recebe: um nome ("call") e poucos campos. Não leva nomes de pessoas nem
 * valores: a tela usa o aviso para buscar de novo o que mudou, pela API, com as regras dela.
 */
public record LiveEvent(String name, Map<String, String> data) {

    public LiveEvent {
        data = Map.copyOf(data);
    }

    /** Para a equipe: qualquer evento do restaurante. */
    public static Optional<LiveEvent> forStaff(DomainEvent event) {
        return switch (event) {
            case OrderChanged o -> Optional.of(new LiveEvent("order", Map.of(
                    "orderId", o.orderId().value().toString(),
                    "tableId", o.tableId().value().toString(),
                    "status", o.status().name())));
            case CallChanged c -> Optional.of(new LiveEvent("call", Map.of(
                    "callId", c.callId().value().toString(),
                    "tableId", c.tableId().value().toString(),
                    "type", c.type().name(),
                    "status", c.status().name())));
            default -> Optional.empty();
        };
    }

    /** Para a mesa: o mesmo aviso, sem o id da mesa (ela já sabe qual é). */
    public static Optional<LiveEvent> forTable(DomainEvent event) {
        return switch (event) {
            case OrderChanged o -> Optional.of(new LiveEvent("order", Map.of(
                    "orderId", o.orderId().value().toString(),
                    "status", o.status().name())));
            case CallChanged c -> Optional.of(new LiveEvent("call", Map.of(
                    "callId", c.callId().value().toString(),
                    "type", c.type().name(),
                    "status", c.status().name())));
            default -> Optional.empty();
        };
    }
}
