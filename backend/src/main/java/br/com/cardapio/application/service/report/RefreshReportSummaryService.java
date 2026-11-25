package br.com.cardapio.application.service.report;

import br.com.cardapio.application.port.in.report.RefreshReportSummaryUseCase;
import br.com.cardapio.application.port.out.report.ReportSummaryPort;
import br.com.cardapio.domain.report.model.SummaryStatus;
import br.com.cardapio.domain.report.service.ReportSummaryPolicy;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Mantém o resumo diário em dia. Cada restaurante é resumido até ontem, no fuso dele: hoje ainda está
 * acontecendo e fica sempre com as consultas diretas.
 */
public class RefreshReportSummaryService implements RefreshReportSummaryUseCase {

    private final ReportSummaryPort summary;
    private final Clock clock;

    public RefreshReportSummaryService(ReportSummaryPort summary, Clock clock) {
        this.summary = summary;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SummaryStatus> due() {
        Instant now = clock.instant();
        return summary.statuses().stream()
                .filter(status -> status.summarizedUntil().isPresent() || status.firstActivityDay().isPresent())
                .filter(status -> {
                    LocalDate today = LocalDate.ofInstant(now, status.zone());
                    return status.summarizedUntil().map(until -> until.isBefore(today)).orElse(true);
                })
                .toList();
    }

    @Override
    @Transactional
    public void refresh(SummaryStatus status) {
        Instant now = clock.instant();
        LocalDate today = LocalDate.ofInstant(now, status.zone());
        LocalDate from = ReportSummaryPolicy.rebuildFrom(status.summarizedUntil(), status.firstActivityDay())
                .orElse(today);
        summary.rebuild(status.establishmentId(), status.zone(), from.isAfter(today) ? today : from, today, now);
    }
}
