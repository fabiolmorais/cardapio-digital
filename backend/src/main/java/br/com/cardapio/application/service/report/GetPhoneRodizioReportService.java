package br.com.cardapio.application.service.report;

import br.com.cardapio.application.port.in.report.GetPhoneRodizioReportUseCase;
import br.com.cardapio.application.port.in.report.PhoneRodizioReport;
import br.com.cardapio.application.port.in.report.ReportQuery;
import br.com.cardapio.application.port.out.account.EstablishmentTimeZonePort;
import br.com.cardapio.application.port.out.floor.SectorPort;
import br.com.cardapio.application.port.out.plan.SubscriptionPort;
import br.com.cardapio.application.port.out.report.PhoneRodizioReportPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.report.service.PhoneRodizioAnalysis;
import br.com.cardapio.domain.rodizio.model.RodizioType;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/** Pedido pelo celular e rodízio (14.7): o relatório inteiro é do Business. */
public class GetPhoneRodizioReportService implements GetPhoneRodizioReportUseCase {

    private final SubscriptionPort subscriptions;
    private final EstablishmentTimeZonePort timeZones;
    private final SectorPort sectors;
    private final PhoneRodizioReportPort report;
    private final Clock clock;

    public GetPhoneRodizioReportService(SubscriptionPort subscriptions, EstablishmentTimeZonePort timeZones,
                                        SectorPort sectors, PhoneRodizioReportPort report, Clock clock) {
        this.subscriptions = subscriptions;
        this.timeZones = timeZones;
        this.sectors = sectors;
        this.report = report;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public PhoneRodizioReport get(ReportQuery query) {
        ReportScope scope = ReportScope.resolve(query, subscriptions, timeZones, sectors, clock);
        scope.access().requireFull();
        EstablishmentId id = query.establishmentId();
        var period = scope.period();
        Instant start = period.start();
        Instant end = period.end();
        var sector = scope.sector();
        return new PhoneRodizioReport(period, PhoneRodizioAnalysis.grouping(period),
                PhoneRodizioAnalysis.origin(period, report.originByDay(id, start, end, period.zone(), sector)),
                report.funnel(id, start, end, sector),
                report.phones(id, start, end, scope.now(), period.zone(), sector),
                report.presence(id, start, end, sector),
                report.rodizio(id, start, end, sector),
                report.tiers(id, start, end, sector),
                report.topItems(id, start, end, sector, Optional.empty(), PhoneRodizioAnalysis.TOP_ITEMS),
                PhoneRodizioAnalysis.cardTime(report.cardTracks(id, start, end,
                        PhoneRodizioAnalysis.CARD_HISTORY_SINCE, sector)),
                report.topItems(id, start, end, sector, Optional.of(RodizioType.PASSAGEM),
                        PhoneRodizioAnalysis.TOP_CUTS));
    }
}
