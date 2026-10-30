package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.in.menu.CreateMenuCategoryUseCase;
import br.com.cardapio.application.port.in.menu.MenuCategorySummary;
import br.com.cardapio.application.port.out.menu.MenuCategoryOverview;
import br.com.cardapio.application.port.out.menu.MenuCategoryPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.exception.MenuCategoryNameTakenException;
import br.com.cardapio.domain.menu.model.MenuCategory;
import org.springframework.transaction.annotation.Transactional;

public class CreateMenuCategoryService implements CreateMenuCategoryUseCase {

    private final MenuCategoryPort categories;

    public CreateMenuCategoryService(MenuCategoryPort categories) {
        this.categories = categories;
    }

    @Override
    @Transactional
    public MenuCategorySummary create(EstablishmentId establishmentId, String name) {
        MenuCategory category = MenuCategory.create(establishmentId, name, categories.nextPosition(establishmentId));
        if (categories.activeNameExists(establishmentId, category.name(), null)) {
            throw new MenuCategoryNameTakenException(category.name());
        }
        categories.save(category);
        return MenuCategorySummary.from(new MenuCategoryOverview(category, 0));
    }
}
