package br.com.cardapio.application.port.in.report;

import br.com.cardapio.domain.menu.model.MenuCategoryId;

import java.util.Objects;
import java.util.Optional;

/**
 * O pedido do relatório da cozinha: o de sempre (restaurante, dias, setor) e, se escolhida, a categoria.
 *
 * @param category null = todas
 */
public record KitchenReportQuery(ReportQuery report, MenuCategoryId category) {

    public KitchenReportQuery {
        Objects.requireNonNull(report, "relatório é obrigatório");
    }

    public Optional<MenuCategoryId> categoryFilter() {
        return Optional.ofNullable(category);
    }
}
