package br.com.cardapio.application.service.order;

import br.com.cardapio.application.port.in.order.ListPrintJobsUseCase;
import br.com.cardapio.application.port.in.order.PrintJob;
import br.com.cardapio.application.port.out.order.KitchenPrintPort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.order.model.TicketKind;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;

public class ListPrintJobsService implements ListPrintJobsUseCase {

    /** Até quando dá para reimprimir: um turno inteiro, com folga. */
    static final Duration REPRINT_WINDOW = Duration.ofHours(12);

    private final KitchenPrintPort printing;
    private final Clock clock;

    public ListPrintJobsService(KitchenPrintPort printing, Clock clock) {
        this.printing = printing;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPage<PrintJob> listPending(EstablishmentId establishmentId, TicketKind kind, PageQuery page) {
        return printing.findPending(establishmentId, kind, page).map(view -> PrintJob.from(view, kind));
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPage<PrintJob> listRecentlyPrinted(EstablishmentId establishmentId, PageQuery page) {
        return printing.findPrintedSince(establishmentId, clock.instant().minus(REPRINT_WINDOW), page)
                .map(view -> PrintJob.from(view, TicketKind.ORDER));
    }
}
