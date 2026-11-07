package br.com.cardapio.application.port.in.order;

import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.order.model.TicketKind;

public interface ListPrintJobsUseCase {

    /** As comandas que a impressora da cozinha ainda não imprimiu, da mais antiga à mais nova. */
    ResultPage<PrintJob> listPending(EstablishmentId establishmentId, TicketKind kind, PageQuery page);

    /** As comandas impressas nas últimas horas, da mais recente para a mais antiga (para reimprimir). */
    ResultPage<PrintJob> listRecentlyPrinted(EstablishmentId establishmentId, PageQuery page);
}
