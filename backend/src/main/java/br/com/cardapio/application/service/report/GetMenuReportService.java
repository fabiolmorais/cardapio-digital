package br.com.cardapio.application.service.report;

import br.com.cardapio.application.port.in.report.GetMenuReportUseCase;
import br.com.cardapio.application.port.in.report.MenuReport;
import br.com.cardapio.application.port.in.report.MenuReportQuery;
import br.com.cardapio.application.port.out.account.EstablishmentTimeZonePort;
import br.com.cardapio.application.port.out.floor.SectorPort;
import br.com.cardapio.application.port.out.menu.MenuCategoryPort;
import br.com.cardapio.application.port.out.plan.SubscriptionPort;
import br.com.cardapio.application.port.out.report.MenuReportPort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.exception.MenuCategoryNotFoundException;
import br.com.cardapio.domain.report.model.CategorySales;
import br.com.cardapio.domain.report.model.ProductSales;
import br.com.cardapio.domain.report.model.ProductSort;
import br.com.cardapio.domain.report.service.MenuAnalysis;
import br.com.cardapio.domain.shared.model.Money;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Objects;

/** O Cardápio (14.3), com os blocos básicos: o Pro em diante. */
public class GetMenuReportService implements GetMenuReportUseCase {

    private final SubscriptionPort subscriptions;
    private final EstablishmentTimeZonePort timeZones;
    private final SectorPort sectors;
    private final MenuCategoryPort categories;
    private final MenuReportPort menu;
    private final Clock clock;

    public GetMenuReportService(SubscriptionPort subscriptions, EstablishmentTimeZonePort timeZones,
                                SectorPort sectors, MenuCategoryPort categories, MenuReportPort menu, Clock clock) {
        this.subscriptions = subscriptions;
        this.timeZones = timeZones;
        this.sectors = sectors;
        this.categories = categories;
        this.menu = menu;
        this.clock = clock;
    }

    /** O de sempre (plano, período, setor) e a categoria: de outro restaurante, 404. */
    private ReportScope scope(MenuReportQuery query) {
        ReportScope scope = ReportScope.resolve(query.report(), subscriptions, timeZones, sectors, clock);
        EstablishmentId id = query.report().establishmentId();
        query.categoryFilter().ifPresent(category -> categories.find(id, category)
                .orElseThrow(MenuCategoryNotFoundException::new));
        return scope;
    }

    @Override
    @Transactional(readOnly = true)
    public MenuReport get(MenuReportQuery query) {
        ReportScope scope = scope(query);
        EstablishmentId id = query.report().establishmentId();
        var period = scope.period();
        List<CategorySales> byCategory = menu.byCategory(id, period.start(), period.end(), scope.sector(),
                query.categoryFilter());
        List<ProductSales> slow = menu.slowSellers(id, period.start(), period.end(), scope.sector(),
                query.categoryFilter(), MenuAnalysis.slowSellerCutoff(scope.now()), MenuAnalysis.SLOW_SELLERS);
        return new MenuReport(scope.access().level(), period,
                Money.ofCents(byCategory.stream().mapToLong(c -> c.revenue().cents()).sum()),
                byCategory.stream().mapToInt(CategorySales::quantity).sum(),
                menu.activeProducts(id, query.categoryFilter()), byCategory, slow);
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPage<ProductSales> products(MenuReportQuery query, ProductSort sort, PageQuery page) {
        Objects.requireNonNull(sort, "ordem é obrigatória");
        ReportScope scope = scope(query);
        var period = scope.period();
        return menu.products(query.report().establishmentId(), period.start(), period.end(), scope.sector(),
                query.categoryFilter(), sort, page);
    }
}
