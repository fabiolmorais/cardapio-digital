package br.com.cardapio.application.port.in.floor;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.floor.model.SectorId;

import java.util.Objects;

/** label, sectorId e active são opcionais: null quer dizer "não mudar". */
public record UpdateTableCommand(EstablishmentId establishmentId, DiningTableId tableId, String label,
                                 SectorId sectorId, Boolean active) {

    public UpdateTableCommand {
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(tableId);
    }
}
