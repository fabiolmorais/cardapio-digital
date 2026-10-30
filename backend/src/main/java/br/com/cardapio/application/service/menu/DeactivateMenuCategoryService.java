package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.in.menu.DeactivateMenuCategoryUseCase;
import br.com.cardapio.application.port.out.menu.MenuCategoryPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.exception.MenuCategoryNotEmptyException;
import br.com.cardapio.domain.menu.exception.MenuCategoryNotFoundException;
import br.com.cardapio.domain.menu.model.MenuCategory;
import br.com.cardapio.domain.menu.model.MenuCategoryId;
import org.springframework.transaction.annotation.Transactional;

public class DeactivateMenuCategoryService implements DeactivateMenuCategoryUseCase {

    private final MenuCategoryPort categories;

    public DeactivateMenuCategoryService(MenuCategoryPort categories) {
        this.categories = categories;
    }

    @Override
    @Transactional
    public void deactivate(EstablishmentId establishmentId, MenuCategoryId categoryId) {
        MenuCategory category = categories.find(establishmentId, categoryId)
                .orElseThrow(MenuCategoryNotFoundException::new);
        if (!category.active()) {
            return; // já estava: o DELETE pode ser repetido sem erro
        }
        long activeProducts = categories.countActiveProducts(establishmentId, categoryId);
        if (activeProducts > 0) {
            throw new MenuCategoryNotEmptyException(activeProducts);
        }
        categories.save(category.deactivate());
    }
}
