package br.com.cardapio.application.port.in.menu;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.menu.model.ProductMediaId;

public interface RemoveProductMediaUseCase {

    void remove(EstablishmentId establishmentId, ProductId productId, ProductMediaId mediaId);
}
