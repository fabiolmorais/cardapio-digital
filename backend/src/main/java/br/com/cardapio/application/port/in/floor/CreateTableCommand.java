package br.com.cardapio.application.port.in.floor;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.SectorId;

import java.util.Objects;

public record CreateTableCommand(EstablishmentId establishmentId, SectorId sectorId, String label) {

    public CreateTableCommand {
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(sectorId);
    }
}
