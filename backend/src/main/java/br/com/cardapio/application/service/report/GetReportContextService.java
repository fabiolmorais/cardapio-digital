package br.com.cardapio.application.service.report;

import br.com.cardapio.application.port.in.report.GetReportContextUseCase;
import br.com.cardapio.application.port.in.report.ReportContext;
import br.com.cardapio.application.port.out.account.EstablishmentTimeZonePort;
import br.com.cardapio.application.port.out.plan.SubscriptionPort;
import br.com.cardapio.application.port.out.report.ReportActivityPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.report.model.ReportAccess;
import br.com.cardapio.domain.report.model.ReportPeriod;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/** O contexto vale para todos os planos: o Essencial também abre a tela, para ver o que o Pro traz. */
public class GetReportContextService implements GetReportContextUseCase {

    private final SubscriptionPort subscriptions;
    private final EstablishmentTimeZonePort timeZones;
    private final ReportActivityPort activity;
    private final Clock clock;

    public GetReportContextService(SubscriptionPort subscriptions, EstablishmentTimeZonePort timeZones,
                                   ReportActivityPort activity, Clock clock) {
        this.subscriptions = subscriptions;
        this.timeZones = timeZones;
        this.activity = activity;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public ReportContext get(EstablishmentId establishmentId) {
        ReportAccess access = ReportAccesses.of(subscriptions, establishmentId);
        ZoneId zone = timeZones.timeZoneOf(establishmentId);
        LocalDate today = LocalDate.ofInstant(clock.instant(), zone);
        LocalDate firstDataDay = activity.firstClosedTabAt(establishmentId)
                .map(at -> LocalDate.ofInstant(at, zone))
                .orElse(null);
        return new ReportContext(access.level(), access.canExport(), zone, today,
                access.earliestDay(today).orElse(null), firstDataDay, ReportPeriod.MAX_DAYS);
    }
}
