package br.com.cardapio.application.port.in.order;

import br.com.cardapio.application.port.out.order.PrintJobView;
import br.com.cardapio.domain.order.model.OrderId;
import br.com.cardapio.domain.order.model.OrderItemId;
import br.com.cardapio.domain.order.model.TicketKind;

import java.time.Instant;
import java.util.List;

/**
 * Uma comanda para a impressora da cozinha: a mesa, o número do pedido e os itens.
 *
 * @param orderedAt quando o pedido chegou (o primeiro item)
 */
public record PrintJob(OrderId orderId, TicketKind kind, int orderNumber, String tableLabel, String sectorName,
                       Instant orderedAt, List<Line> items) {

    /**
     * Um item na comanda.
     *
     * @param rodizio veio numa rodada do rodízio (sem preço)
     * @param cancelReason o motivo, na via "CANCELADO"
     */
    public record Line(OrderItemId id, String productName, int quantity, String note, boolean rodizio,
                       String cancelReason) {
    }

    public PrintJob {
        items = List.copyOf(items);
    }

    public static PrintJob from(PrintJobView view, TicketKind kind) {
        Instant orderedAt = view.items().stream().map(i -> i.createdAt()).min(Instant::compareTo).orElse(null);
        return new PrintJob(view.orderId(), kind, view.orderNumber(), view.tableLabel(), view.sectorName(), orderedAt,
                view.items().stream().map(i -> new Line(i.id(), i.productName(), i.quantity(), i.note(), i.inRodizio(),
                        kind == TicketKind.CANCELLATION ? i.cancelReason() : null)).toList());
    }
}
