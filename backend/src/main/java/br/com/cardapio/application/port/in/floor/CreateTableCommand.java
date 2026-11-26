package br.com.cardapio.application.port.in.floor;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.SectorId;
import br.com.cardapio.domain.floor.model.TableKind;

import java.util.Objects;

public record CreateTableCommand(EstablishmentId establishmentId, SectorId sectorId, String label, TableKind kind) {

    public CreateTableCommand {
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(sectorId);
        kind = kind == null ? TableKind.TABLE : kind;
    }

    /** Uma mesa (o balcão passa o tipo). */
    public CreateTableCommand(EstablishmentId establishmentId, SectorId sectorId, String label) {
        this(establishmentId, sectorId, label, TableKind.TABLE);
    }
}
