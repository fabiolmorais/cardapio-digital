package br.com.cardapio.application.service.report;

import br.com.cardapio.application.port.in.report.GetKitchenReportUseCase;
import br.com.cardapio.application.port.in.report.KitchenReport;
import br.com.cardapio.application.port.in.report.KitchenReportQuery;
import br.com.cardapio.application.port.out.account.EstablishmentTimeZonePort;
import br.com.cardapio.application.port.out.floor.SectorPort;
import br.com.cardapio.application.port.out.menu.MenuCategoryPort;
import br.com.cardapio.application.port.out.plan.SubscriptionPort;
import br.com.cardapio.application.port.out.report.KitchenReportPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.exception.MenuCategoryNotFoundException;
import br.com.cardapio.domain.report.model.PrepTime;
import br.com.cardapio.domain.report.service.KitchenAnalysis;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

/** Cozinha e tempos (14.4), com os blocos básicos: o Pro em diante. */
public class GetKitchenReportService implements GetKitchenReportUseCase {

    private final SubscriptionPort subscriptions;
    private final EstablishmentTimeZonePort timeZones;
    private final SectorPort sectors;
    private final MenuCategoryPort categories;
    private final KitchenReportPort kitchen;
    private final Clock clock;

    public GetKitchenReportService(SubscriptionPort subscriptions, EstablishmentTimeZonePort timeZones,
                                   SectorPort sectors, MenuCategoryPort categories, KitchenReportPort kitchen,
                                   Clock clock) {
        this.subscriptions = subscriptions;
        this.timeZones = timeZones;
        this.sectors = sectors;
        this.categories = categories;
        this.kitchen = kitchen;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public KitchenReport get(KitchenReportQuery query) {
        ReportScope scope = ReportScope.resolve(query.report(), subscriptions, timeZones, sectors, clock);
        EstablishmentId id = query.report().establishmentId();
        // Categoria de outro restaurante: 404, sem revelar que existe.
        query.categoryFilter().ifPresent(category -> categories.find(id, category)
                .orElseThrow(MenuCategoryNotFoundException::new));
        var period = scope.period();
        var comparison = scope.comparison();
        List<PrepTime> times = kitchen.prepTimes(id, period.start(), period.end(), period.zone(), scope.sector(),
                query.categoryFilter());
        List<PrepTime> compared = kitchen.prepTimes(id, comparison.start(), scope.comparisonEnd(), period.zone(),
                scope.sector(), query.categoryFilter());
        return new KitchenReport(scope.access().level(), period, comparison, KitchenAnalysis.average(times),
                KitchenAnalysis.average(compared), times.size(), KitchenAnalysis.slowest(times, KitchenAnalysis.SLOWEST));
    }
}
