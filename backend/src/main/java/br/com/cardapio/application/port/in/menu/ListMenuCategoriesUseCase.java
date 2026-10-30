package br.com.cardapio.application.port.in.menu;

import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;

public interface ListMenuCategoriesUseCase {

    ResultPage<MenuCategorySummary> list(EstablishmentId establishmentId, PageQuery page);
}
