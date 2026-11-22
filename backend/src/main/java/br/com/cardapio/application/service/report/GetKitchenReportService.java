package br.com.cardapio.application.service.report;

import br.com.cardapio.application.port.in.report.GetKitchenReportUseCase;
import br.com.cardapio.application.port.in.report.KitchenFull;
import br.com.cardapio.application.port.in.report.KitchenReport;
import br.com.cardapio.application.port.in.report.KitchenReportQuery;
import br.com.cardapio.application.port.out.account.EstablishmentTimeZonePort;
import br.com.cardapio.application.port.out.floor.SectorPort;
import br.com.cardapio.application.port.out.menu.MenuCategoryPort;
import br.com.cardapio.application.port.out.plan.SubscriptionPort;
import br.com.cardapio.application.port.out.report.KitchenReportPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.exception.MenuCategoryNotFoundException;
import br.com.cardapio.domain.report.model.KitchenItem;
import br.com.cardapio.domain.report.model.PrepTime;
import br.com.cardapio.domain.report.model.ReportLevel;
import br.com.cardapio.domain.report.service.KitchenAnalysis;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.ZoneId;
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
        boolean business = scope.access().level().covers(ReportLevel.FULL);
        // No Business, os itens vêm inteiros (o pronto e a entrega): os blocos completos saem da mesma lista.
        List<KitchenItem> items = business ? kitchen.items(id, period.start(), period.end(), scope.sector(),
                query.categoryFilter()) : List.of();
        List<KitchenItem> comparedItems = business ? kitchen.items(id, comparison.start(), scope.comparisonEnd(),
                scope.sector(), query.categoryFilter()) : List.of();
        List<PrepTime> times = business ? KitchenAnalysis.prepTimes(items, period.zone())
                : kitchen.prepTimes(id, period.start(), period.end(), period.zone(), scope.sector(),
                query.categoryFilter());
        List<PrepTime> compared = business ? KitchenAnalysis.prepTimes(comparedItems, period.zone())
                : kitchen.prepTimes(id, comparison.start(), scope.comparisonEnd(), period.zone(), scope.sector(),
                query.categoryFilter());
        return new KitchenReport(scope.access().level(), period, comparison, KitchenAnalysis.average(times),
                KitchenAnalysis.average(compared), times.size(), KitchenAnalysis.slowest(times, KitchenAnalysis.SLOWEST),
                business ? full(id, scope, items, comparedItems, times) : null);
    }

    /** Os blocos do Business. Quem só tem os básicos não recebe nada disto (o dado não sai do servidor). */
    private KitchenFull full(EstablishmentId id, ReportScope scope, List<KitchenItem> items,
                             List<KitchenItem> comparedItems, List<PrepTime> times) {
        var period = scope.period();
        ZoneId zone = period.zone();
        return new KitchenFull(
                KitchenAnalysis.serviceTimes(items, kitchen.lateConfirmations(id, period.start(), period.end(),
                        scope.sector(), KitchenAnalysis.LATE_CONFIRMATION)),
                KitchenAnalysis.serviceTimes(comparedItems, kitchen.lateConfirmations(id, scope.comparison().start(),
                        scope.comparisonEnd(), scope.sector(), KitchenAnalysis.LATE_CONFIRMATION)),
                KitchenAnalysis.byHour(items, zone),
                KitchenAnalysis.peakAverages(KitchenAnalysis.peaks(items, zone)),
                KitchenAnalysis.byCategory(times),
                KitchenAnalysis.delayed(items, zone, KitchenAnalysis.LATE_PASSE, KitchenAnalysis.DELAYED_SLOTS));
    }
}
