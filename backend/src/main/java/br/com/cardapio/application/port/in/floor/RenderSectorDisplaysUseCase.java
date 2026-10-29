package br.com.cardapio.application.port.in.floor;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.SectorId;

public interface RenderSectorDisplaysUseCase {

    /** Um PDF com o display de cada mesa ativa do setor, uma por página. */
    RenderedFile render(EstablishmentId establishmentId, SectorId sectorId);
}
