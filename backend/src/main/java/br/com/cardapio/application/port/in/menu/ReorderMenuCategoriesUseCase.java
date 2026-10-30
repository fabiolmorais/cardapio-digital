package br.com.cardapio.application.port.in.menu;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.MenuCategoryId;

import java.util.List;

public interface ReorderMenuCategoriesUseCase {

    /** ids: todas as categorias ativas, na ordem nova. */
    void reorder(EstablishmentId establishmentId, List<MenuCategoryId> ids);
}
