package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.in.menu.ListMenuCategoriesUseCase;
import br.com.cardapio.application.port.in.menu.MenuCategorySummary;
import br.com.cardapio.application.port.out.menu.MenuCategoryPort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import org.springframework.transaction.annotation.Transactional;

public class ListMenuCategoriesService implements ListMenuCategoriesUseCase {

    private final MenuCategoryPort categories;

    public ListMenuCategoriesService(MenuCategoryPort categories) {
        this.categories = categories;
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPage<MenuCategorySummary> list(EstablishmentId establishmentId, PageQuery page) {
        return categories.findOverview(establishmentId, page).map(MenuCategorySummary::from);
    }
}
