package br.com.cardapio.adapter.in.scheduler;

import br.com.cardapio.application.port.in.report.SendReportAlertsUseCase;
import br.com.cardapio.domain.account.model.EstablishmentId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** De minuto em minuto, os alertas em tempo real (REL-78): cozinha atrasando, chamado sem resposta, comanda. */
@Component
class ReportAlertsJob {

    private static final Logger log = LoggerFactory.getLogger(ReportAlertsJob.class);

    private final SendReportAlertsUseCase alerts;

    ReportAlertsJob(SendReportAlertsUseCase alerts) {
        this.alerts = alerts;
    }

    @Scheduled(fixedDelayString = "${app.reports.alerts-interval:PT1M}",
            initialDelayString = "${app.reports.alerts-interval:PT1M}")
    void run() {
        int sent = 0;
        for (EstablishmentId id : alerts.candidates()) {
            try {
                sent += alerts.check(id);
            } catch (RuntimeException e) {
                // Um restaurante com problema não segura os outros; o id não é dado pessoal.
                log.warn("Alertas não conferidos para o restaurante {}", id.value(), e);
            }
        }
        if (sent > 0) {
            log.info("Alertas em tempo real enviados: {}", sent);
        }
    }
}
