package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.in.menu.MenuCategorySummary;
import br.com.cardapio.application.port.in.menu.RenameMenuCategoryUseCase;
import br.com.cardapio.application.port.out.menu.MenuCategoryOverview;
import br.com.cardapio.application.port.out.menu.MenuCategoryPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.exception.MenuCategoryNameTakenException;
import br.com.cardapio.domain.menu.model.MenuCategory;
import br.com.cardapio.domain.menu.model.MenuCategoryId;
import org.springframework.transaction.annotation.Transactional;

public class RenameMenuCategoryService implements RenameMenuCategoryUseCase {

    private final MenuCategoryPort categories;

    public RenameMenuCategoryService(MenuCategoryPort categories) {
        this.categories = categories;
    }

    @Override
    @Transactional
    public MenuCategorySummary rename(EstablishmentId establishmentId, MenuCategoryId categoryId, String newName) {
        MenuCategory renamed = MenuLookups.activeCategory(categories, establishmentId, categoryId).rename(newName);
        if (categories.activeNameExists(establishmentId, renamed.name(), categoryId)) {
            throw new MenuCategoryNameTakenException(renamed.name());
        }
        categories.save(renamed);
        return MenuCategorySummary.from(new MenuCategoryOverview(renamed,
                categories.countActiveProducts(establishmentId, categoryId)));
    }
}
