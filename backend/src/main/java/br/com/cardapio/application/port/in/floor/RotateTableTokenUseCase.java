package br.com.cardapio.application.port.in.floor;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.DiningTableId;

public interface RotateTableTokenUseCase {

    /** "Trocar QR desta mesa": token novo; o QR impresso com o antigo deixa de funcionar. */
    TableSummary rotate(EstablishmentId establishmentId, DiningTableId tableId);
}
