package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.in.menu.ReorderMenuCategoriesUseCase;
import br.com.cardapio.application.port.out.menu.MenuCategoryPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.MenuCategory;
import br.com.cardapio.domain.menu.model.MenuCategoryId;
import br.com.cardapio.domain.shared.model.DisplayOrder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class ReorderMenuCategoriesService implements ReorderMenuCategoriesUseCase {

    private final MenuCategoryPort categories;

    public ReorderMenuCategoriesService(MenuCategoryPort categories) {
        this.categories = categories;
    }

    @Override
    @Transactional
    public void reorder(EstablishmentId establishmentId, List<MenuCategoryId> ids) {
        DisplayOrder<MenuCategoryId> order = new DisplayOrder<>(ids);
        List<MenuCategory> current = categories.findActive(establishmentId);
        order.requireSameItemsAs(current.stream().map(MenuCategory::id).toList());

        // Só grava quem mudou de lugar: arrastar um item costuma mexer em poucos.
        for (MenuCategory category : current) {
            int position = order.positionOf(category.id());
            if (position != category.position()) {
                categories.save(category.at(position));
            }
        }
    }
}
