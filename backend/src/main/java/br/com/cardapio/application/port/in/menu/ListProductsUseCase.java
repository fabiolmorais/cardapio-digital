package br.com.cardapio.application.port.in.menu;

import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.MenuCategoryId;

public interface ListProductsUseCase {

    /** categoryId e text são opcionais (null: sem filtro). */
    ResultPage<ProductSummary> list(
            EstablishmentId establishmentId,
            MenuCategoryId categoryId,
            String text,
            PageQuery page);
}
