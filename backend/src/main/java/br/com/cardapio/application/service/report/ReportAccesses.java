package br.com.cardapio.application.service.report;

import br.com.cardapio.application.port.out.plan.SubscriptionPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.report.model.ReportAccess;

/** O acesso aos relatórios pelo plano do restaurante: todo caso de uso de relatório começa por aqui. */
public final class ReportAccesses {

    private ReportAccesses() {
    }

    public static ReportAccess of(SubscriptionPort subscriptions, EstablishmentId establishmentId) {
        // Todo cadastro ganha a assinatura (e a migração V26 deu uma a quem já existia).
        return subscriptions.find(establishmentId)
                .map(current -> ReportAccess.of(current.plan()))
                .orElseThrow(() -> new IllegalStateException("restaurante sem assinatura"));
    }
}
