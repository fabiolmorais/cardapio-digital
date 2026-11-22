package br.com.cardapio.application.service.report;

import br.com.cardapio.application.port.in.report.GetMenuReportUseCase;
import br.com.cardapio.application.port.in.report.MenuFull;
import br.com.cardapio.application.port.in.report.MenuProduct;
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
import br.com.cardapio.domain.report.model.OptionSales;
import br.com.cardapio.domain.report.model.ProductRow;
import br.com.cardapio.domain.report.model.ProductSales;
import br.com.cardapio.domain.report.model.ReportLevel;
import br.com.cardapio.domain.report.model.ProductSort;
import br.com.cardapio.domain.report.service.MenuAnalysis;
import br.com.cardapio.domain.shared.model.Money;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Objects;

/** O Cardápio (14.3), com os blocos básicos: o Pro em diante. */
public class GetMenuReportService implements GetMenuReportUseCase {

    /** Quantos pares o banco devolve para a regra escolher os que mais aparecem juntos. */
    static final int PAIR_CANDIDATES = 20;

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
                menu.activeProducts(id, query.categoryFilter()), byCategory, slow,
                scope.access().level().covers(ReportLevel.FULL) ? full(id, scope, query) : null);
    }

    /** Os blocos do Business. Quem só tem os básicos não recebe nada disto (o dado não sai do servidor). */
    private MenuFull full(EstablishmentId id, ReportScope scope, MenuReportQuery query) {
        var period = scope.period();
        var category = query.categoryFilter();
        List<OptionSales> options = menu.options(id, period.start(), period.end(), scope.sector(), category);
        return new MenuFull(
                MenuAnalysis.mostCanceled(menu.canceled(id, period.start(), period.end(), scope.sector(), category),
                        MenuAnalysis.TOP_CANCELED),
                MenuAnalysis.sizes(menu.sizes(id, period.start(), period.end(), scope.sector(), category),
                        MenuAnalysis.TOP_SIZES),
                MenuAnalysis.topOptions(options, MenuAnalysis.TOP_OPTIONS),
                MenuAnalysis.paidOptions(options),
                // Os pares olham o cardápio todo: o par bom costuma juntar categorias (o prato e a bebida).
                MenuAnalysis.pairs(menu.pairs(id, period.start(), period.end(), scope.sector(),
                        MenuAnalysis.MIN_TOGETHER, PAIR_CANDIDATES), MenuAnalysis.TOP_PAIRS),
                menu.video(id, period.start(), period.end(), scope.sector(), category));
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPage<MenuProduct> products(MenuReportQuery query, ProductSort sort, PageQuery page) {
        Objects.requireNonNull(sort, "ordem é obrigatória");
        ReportScope scope = scope(query);
        boolean full = scope.access().level().covers(ReportLevel.FULL);
        if (sort == ProductSort.GROWTH) {
            scope.access().requireFull();
        }
        var period = scope.period();
        ResultPage<ProductRow> rows = menu.products(query.report().establishmentId(), period.start(), period.end(),
                scope.comparison().start(), scope.comparisonEnd(), scope.sector(), query.categoryFilter(), sort, page);
        return new ResultPage<>(rows.content().stream().map(full ? MenuProduct::full : MenuProduct::basic).toList(),
                rows.page(), rows.size(), rows.totalElements());
    }
}
