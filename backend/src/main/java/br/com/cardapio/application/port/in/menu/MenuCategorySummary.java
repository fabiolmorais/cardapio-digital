package br.com.cardapio.application.port.in.menu;

import br.com.cardapio.application.port.out.menu.MenuCategoryOverview;
import br.com.cardapio.domain.menu.model.MenuCategoryId;

public record MenuCategorySummary(
        MenuCategoryId id,
        String name,
        int position,
        long activeProducts
) {

    public static MenuCategorySummary from(MenuCategoryOverview overview) {
        return new MenuCategorySummary(overview.category().id(), overview.category().name(),
                overview.category().position(), overview.activeProducts());
    }
}
