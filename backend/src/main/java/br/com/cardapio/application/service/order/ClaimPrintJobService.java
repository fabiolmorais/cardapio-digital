package br.com.cardapio.application.service.order;

import br.com.cardapio.application.port.in.order.ClaimPrintJobUseCase;
import br.com.cardapio.application.port.in.order.PrintJob;
import br.com.cardapio.application.port.out.order.KitchenPrintPort;
import br.com.cardapio.application.port.out.order.OrderPort;
import br.com.cardapio.application.port.out.order.PrintJobView;
import br.com.cardapio.application.port.out.realtime.EventPublisherPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.order.event.OrderChanged;
import br.com.cardapio.domain.order.exception.OrderNotFoundException;
import br.com.cardapio.domain.order.model.OrderId;
import br.com.cardapio.domain.order.model.OrderItem;
import br.com.cardapio.domain.order.model.OrderStatus;
import br.com.cardapio.domain.order.model.TicketKind;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

public class ClaimPrintJobService implements ClaimPrintJobUseCase {

    private final KitchenPrintPort printing;
    private final OrderPort orders;
    private final EventPublisherPort events;
    private final Clock clock;

    public ClaimPrintJobService(KitchenPrintPort printing, OrderPort orders, EventPublisherPort events, Clock clock) {
        this.printing = printing;
        this.orders = orders;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Trava os itens que faltam, marca como impressos e devolve só eles. Com as linhas travadas, o
     * "em preparo" grava sem disputa; o cliente vê a mudança pelo tempo real.
     */
    @Override
    @Transactional
    public PrintJob claim(EstablishmentId establishmentId, OrderId orderId, TicketKind kind) {
        PrintJobView job = printing.lockPending(establishmentId, orderId, kind)
                .orElseThrow(OrderNotFoundException::new);
        if (job.items().isEmpty()) {
            return PrintJob.from(job, kind);
        }
        Instant now = clock.instant();
        if (kind == TicketKind.ORDER) {
            for (OrderItem item : job.items()) {
                OrderItem printed = item.printedForKitchen(now);
                if (printed != item) {
                    orders.updateItem(printed, item.status());
                }
            }
        }
        printing.markPrinted(establishmentId, job.items().stream().map(OrderItem::id).toList(), kind, now);
        events.publish(new OrderChanged(establishmentId, job.tableId(), orderId, OrderStatus.SENT, now));
        return PrintJob.from(job, kind);
    }
}
