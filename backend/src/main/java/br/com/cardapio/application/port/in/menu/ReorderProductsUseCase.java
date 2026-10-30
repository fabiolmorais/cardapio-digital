package br.com.cardapio.application.port.in.menu;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.MenuCategoryId;
import br.com.cardapio.domain.menu.model.ProductId;

import java.util.List;

public interface ReorderProductsUseCase {

    /** ids: todos os produtos ativos da categoria, na ordem nova. */
    void reorder(EstablishmentId establishmentId, MenuCategoryId categoryId, List<ProductId> ids);
}
