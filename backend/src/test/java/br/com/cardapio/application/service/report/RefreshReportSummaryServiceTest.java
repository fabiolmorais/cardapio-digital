package br.com.cardapio.application.service.report;

import br.com.cardapio.application.port.out.report.ReportSummaryPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.SectorId;
import br.com.cardapio.domain.report.model.ItemSales;
import br.com.cardapio.domain.report.model.PrepTotal;
import br.com.cardapio.domain.report.model.SummaryStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshReportSummaryServiceTest {

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private static final ZoneId NORONHA = ZoneId.of("America/Noronha");
    /** 25/11, 23h30 em São Paulo; em Noronha (uma hora a mais) já é dia 26. */
    private static final Instant NOW = Instant.parse("2026-11-26T02:30:00Z");

    private static LocalDate day(String value) {
        return LocalDate.parse(value);
    }

    private final FakeSummary summary = new FakeSummary();
    private final RefreshReportSummaryService service =
            new RefreshReportSummaryService(summary, Clock.fixed(NOW, ZoneOffset.UTC));

    private SummaryStatus status(ZoneId zone, LocalDate until, LocalDate firstActivity) {
        return new SummaryStatus(EstablishmentId.newId(), zone, Optional.ofNullable(until),
                Optional.ofNullable(firstActivity));
    }

    @Test
    void onlyRestaurantsWhoseDayTurnedOrThatNeverSummarized() {
        SummaryStatus upToDate = status(SAO_PAULO, day("2026-11-25"), null);
        SummaryStatus dayTurned = status(NORONHA, day("2026-11-25"), null);
        SummaryStatus never = status(SAO_PAULO, null, day("2025-03-02"));
        SummaryStatus noActivity = status(SAO_PAULO, null, null);
        summary.statuses.addAll(List.of(upToDate, dayTurned, never, noActivity));

        assertThat(service.due()).containsExactly(dayTurned, never);
    }

    @Test
    void redoesTheLastThreeDaysOrTheWholeHistoryUpToYesterday() {
        service.refresh(status(NORONHA, day("2026-11-25"), null));
        service.refresh(status(SAO_PAULO, null, day("2025-03-02")));

        assertThat(summary.rebuilt).containsExactly(
                "2026-11-22 → 2026-11-26 (America/Noronha)",
                "2025-03-02 → 2026-11-25 (America/Sao_Paulo)");
    }

    private static final class FakeSummary implements ReportSummaryPort {
        final List<SummaryStatus> statuses = new ArrayList<>();
        final List<String> rebuilt = new ArrayList<>();

        @Override
        public Optional<LocalDate> summarizedUntil(EstablishmentId establishmentId) {
            return Optional.empty();
        }

        @Override
        public List<ItemSales> sales(EstablishmentId establishmentId, LocalDate from, LocalDate until,
                                     Optional<SectorId> sector) {
            return List.of();
        }

        @Override
        public List<PrepTotal> preparations(EstablishmentId establishmentId, LocalDate from, LocalDate until,
                                            Optional<SectorId> sector) {
            return List.of();
        }

        @Override
        public List<SummaryStatus> statuses() {
            return statuses;
        }

        @Override
        public void rebuild(EstablishmentId establishmentId, ZoneId zone, LocalDate from, LocalDate until,
                            Instant now) {
            rebuilt.add(from + " → " + until + " (" + zone.getId() + ")");
        }

        @Override
        public void invalidate(EstablishmentId establishmentId) {
        }
    }
}
