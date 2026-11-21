package br.com.cardapio.application.port.in.report;

import br.com.cardapio.domain.menu.model.MenuCategoryId;

import java.util.Objects;
import java.util.Optional;

/**
 * O pedido do relatório de cardápio: o de sempre (restaurante, dias, setor) e, se escolhida, a categoria.
 *
 * @param category null = todas
 */
public record MenuReportQuery(ReportQuery report, MenuCategoryId category) {

    public MenuReportQuery {
        Objects.requireNonNull(report, "relatório é obrigatório");
    }

    public Optional<MenuCategoryId> categoryFilter() {
        return Optional.ofNullable(category);
    }
}
