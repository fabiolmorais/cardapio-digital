package br.com.cardapio.application.port.in.floor;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.DiningTableId;

public interface RenderTableDisplayUseCase {

    RenderedFile render(EstablishmentId establishmentId, DiningTableId tableId, DisplayFormat format);
}
