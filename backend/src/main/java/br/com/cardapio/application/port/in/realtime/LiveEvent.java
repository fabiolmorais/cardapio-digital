package br.com.cardapio.application.port.in.realtime;

import br.com.cardapio.domain.call.event.CallChanged;
import br.com.cardapio.domain.order.event.OrderChanged;
import br.com.cardapio.domain.order.event.TabClosed;
import br.com.cardapio.domain.order.event.TabMoved;
import br.com.cardapio.domain.rating.event.LowRatingReceived;
import br.com.cardapio.domain.rodizio.event.RodizioChanged;
import br.com.cardapio.domain.shared.event.DomainEvent;
import br.com.cardapio.domain.tablesession.event.PhoneBlockChanged;

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
            case TabClosed t -> Optional.of(new LiveEvent("tab", Map.of(
                    "tabId", t.tabId().value().toString(),
                    "tableId", t.tableId().value().toString())));
            case RodizioChanged r -> Optional.of(new LiveEvent("rodizio", Map.of(
                    "tabId", r.tabId().value().toString(),
                    "tableId", r.tableId().value().toString())));
            case PhoneBlockChanged p -> Optional.of(new LiveEvent("phone", Map.of(
                    "tableId", p.tableId().value().toString(),
                    "blocked", String.valueOf(p.blocked()))));
            // Nota baixa: o painel de dono e gerente busca o alerta. Sem o comentário nem quem atendeu.
            case LowRatingReceived r -> Optional.of(new LiveEvent("rating", Map.of(
                    "ratingId", r.ratingId().value().toString(),
                    "tableId", r.tableId().value().toString())));
            // A mudança sai uma vez para cada mesa; para a equipe, basta a da mesa de origem.
            case TabMoved t when t.leftThisTable() -> Optional.of(new LiveEvent("tab-moved", Map.of(
                    "tabId", t.tabId().value().toString(),
                    "fromTableId", t.fromTableId().value().toString(),
                    "toTableId", t.toTableId().value().toString())));
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
            case TabClosed t -> Optional.of(new LiveEvent("tab", Map.of("tabId", t.tabId().value().toString())));
            case RodizioChanged r -> Optional.of(new LiveEvent("rodizio", Map.of("tabId", r.tabId().value().toString())));
            // Sem dizer qual celular: cada um da mesa confere de novo a própria presença.
            case PhoneBlockChanged p -> Optional.of(new LiveEvent("presence", Map.of()));
            // left: a comanda foi embora desta mesa (o celular vai atrás); senão, chegou uma comanda nela.
            case TabMoved t -> Optional.of(new LiveEvent("tab-moved", Map.of(
                    "left", String.valueOf(t.leftThisTable()))));
            default -> Optional.empty();
        };
    }
}
