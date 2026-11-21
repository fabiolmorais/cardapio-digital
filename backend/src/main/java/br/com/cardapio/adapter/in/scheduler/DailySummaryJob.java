package br.com.cardapio.adapter.in.scheduler;

import br.com.cardapio.application.port.in.report.SendDailySummariesUseCase;
import br.com.cardapio.domain.account.model.EstablishmentId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** De 10 em 10 minutos, os resumos diários que estão na vez (cada restaurante no horário dele). */
@Component
class DailySummaryJob {

    private static final Logger log = LoggerFactory.getLogger(DailySummaryJob.class);

    private final SendDailySummariesUseCase summaries;

    DailySummaryJob(SendDailySummariesUseCase summaries) {
        this.summaries = summaries;
    }

    @Scheduled(fixedDelayString = "${app.reports.daily-summary-interval:PT10M}",
            initialDelayString = "${app.reports.daily-summary-interval:PT10M}")
    void run() {
        int sent = 0;
        for (EstablishmentId id : summaries.due()) {
            try {
                if (summaries.send(id)) {
                    sent++;
                }
            } catch (RuntimeException e) {
                // Um restaurante com problema não segura os outros; o id não é dado pessoal.
                log.warn("Resumo diário não enviado para o restaurante {}", id.value(), e);
            }
        }
        if (sent > 0) {
            log.info("Resumos diários enviados: {}", sent);
        }
    }
}
