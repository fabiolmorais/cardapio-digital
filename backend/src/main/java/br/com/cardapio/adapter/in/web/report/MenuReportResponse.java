package br.com.cardapio.adapter.in.web.report;

import br.com.cardapio.adapter.in.web.report.ReportDtos.Category;
import br.com.cardapio.adapter.in.web.report.ReportDtos.Level;
import br.com.cardapio.adapter.in.web.report.ReportDtos.Product;
import br.com.cardapio.adapter.in.web.report.ReportDtos.Range;
import br.com.cardapio.application.port.in.report.MenuReport;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "MenuReport", description = "O Cardápio, com os blocos básicos (a tabela de produtos vem paginada à parte)")
record MenuReportResponse(
        @Schema(requiredMode = REQUIRED) Level level,
        @Schema(requiredMode = REQUIRED) Range period,
        @Schema(requiredMode = REQUIRED, description = "Faturamento dos itens: a base da participação")
        long itemsRevenueCents,
        @Schema(requiredMode = REQUIRED) int itemsSold,
        @Schema(requiredMode = REQUIRED, description = "Produtos ativos do cardápio de sempre") int activeProducts,
        @Schema(requiredMode = REQUIRED) List<Category> byCategory,
        @Schema(requiredMode = REQUIRED, description = "Ativos há mais de 30 dias, do que menos vendeu")
        List<Product> slowSellers) {

    static MenuReportResponse from(MenuReport r) {
        return new MenuReportResponse(Level.valueOf(r.level().name()), Range.of(r.period()), r.itemsRevenue().cents(),
                r.itemsSold(), r.activeProducts(), r.byCategory().stream().map(Category::of).toList(),
                r.slowSellers().stream().map(Product::of).toList());
    }
}
