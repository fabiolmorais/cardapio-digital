package br.com.cardapio.adapter.in.scheduler;

import br.com.cardapio.application.port.in.report.RefreshReportSummaryUseCase;
import br.com.cardapio.domain.report.model.SummaryStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * O resumo diário dos relatórios (ADR 0014): de hora em hora, resume o dia que acabou de cada
 * restaurante (no fuso dele) e refaz os mais recentes. A primeira volta faz o histórico inteiro.
 */
@Component
class ReportSummaryJob {

    private static final Logger log = LoggerFactory.getLogger(ReportSummaryJob.class);

    private final RefreshReportSummaryUseCase summaries;

    ReportSummaryJob(RefreshReportSummaryUseCase summaries) {
        this.summaries = summaries;
    }

    @Scheduled(fixedDelayString = "${app.reports.summary-interval:PT1H}",
            initialDelayString = "${app.reports.summary-initial-delay:PT2M}")
    void run() {
        int done = 0;
        for (SummaryStatus status : summaries.due()) {
            try {
                summaries.refresh(status);
                done++;
            } catch (RuntimeException e) {
                // Um restaurante com problema não segura os outros; o id não é dado pessoal.
                log.warn("Resumo dos relatórios não atualizado para o restaurante {}",
                        status.establishmentId().value(), e);
            }
        }
        if (done > 0) {
            log.info("Resumos dos relatórios atualizados: {}", done);
        }
    }
}
