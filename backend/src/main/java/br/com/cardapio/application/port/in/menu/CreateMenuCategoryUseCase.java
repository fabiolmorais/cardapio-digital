package br.com.cardapio.application.port.in.menu;

import br.com.cardapio.domain.account.model.EstablishmentId;

public interface CreateMenuCategoryUseCase {

    MenuCategorySummary create(EstablishmentId establishmentId, String name);
}
