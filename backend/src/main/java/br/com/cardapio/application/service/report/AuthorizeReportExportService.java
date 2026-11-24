package br.com.cardapio.application.service.report;

import br.com.cardapio.application.port.in.report.AuthorizeReportExportUseCase;
import br.com.cardapio.application.port.out.plan.SubscriptionPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import org.springframework.transaction.annotation.Transactional;

/** A exportação em CSV é do Business (REPORT_EXPORT): os outros planos recebem 403. */
public class AuthorizeReportExportService implements AuthorizeReportExportUseCase {

    private final SubscriptionPort subscriptions;

    public AuthorizeReportExportService(SubscriptionPort subscriptions) {
        this.subscriptions = subscriptions;
    }

    @Override
    @Transactional(readOnly = true)
    public void authorize(EstablishmentId establishmentId) {
        ReportAccesses.of(subscriptions, establishmentId).requireExport();
    }
}
