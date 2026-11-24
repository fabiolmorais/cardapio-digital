package br.com.cardapio.application.service.report;

import br.com.cardapio.application.port.in.report.ManageReportAlertsUseCase;
import br.com.cardapio.application.port.out.plan.SubscriptionPort;
import br.com.cardapio.application.port.out.report.ReportAlertPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.report.model.AlertSettings;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/** Os alertas em tempo real são do Business (relatórios completos): os outros planos recebem 403. */
public class ManageReportAlertsService implements ManageReportAlertsUseCase {

    private final SubscriptionPort subscriptions;
    private final ReportAlertPort alerts;

    public ManageReportAlertsService(SubscriptionPort subscriptions, ReportAlertPort alerts) {
        this.subscriptions = subscriptions;
        this.alerts = alerts;
    }

    @Override
    @Transactional(readOnly = true)
    public AlertSettings get(EstablishmentId establishmentId) {
        ReportAccesses.of(subscriptions, establishmentId).requireFull();
        return alerts.settings(establishmentId).orElse(AlertSettings.DEFAULT);
    }

    @Override
    @Transactional
    public AlertSettings update(EstablishmentId establishmentId, AlertSettings settings) {
        Objects.requireNonNull(settings, "configuração é obrigatória");
        ReportAccesses.of(subscriptions, establishmentId).requireFull();
        alerts.saveSettings(establishmentId, settings);
        return settings;
    }
}
