package br.com.cardapio.application.port.in.menu;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.MenuCategoryId;

public interface RenameMenuCategoryUseCase {

    MenuCategorySummary rename(EstablishmentId establishmentId, MenuCategoryId categoryId, String newName);
}
