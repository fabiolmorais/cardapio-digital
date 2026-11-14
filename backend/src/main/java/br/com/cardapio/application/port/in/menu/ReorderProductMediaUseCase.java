package br.com.cardapio.application.port.in.menu;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.MediaOwner;
import br.com.cardapio.domain.menu.model.ProductMediaId;

import java.util.List;

public interface ReorderProductMediaUseCase {

    /** ids: todas as mídias ativas, na ordem nova. A primeira vira a capa. */
    void reorder(EstablishmentId establishmentId, MediaOwner owner, List<ProductMediaId> ids);
}
