package br.com.cardapio.application.port.in.floor;

import br.com.cardapio.domain.floor.model.DiningTable;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.floor.model.SectorId;

import java.time.Instant;

/** A mesa para o painel. publicToken vai junto porque o painel mostra a prévia do QR. */
public record TableSummary(DiningTableId id, SectorId sectorId, String label, String publicToken,
                           Instant tokenIssuedAt, boolean active) {

    public static TableSummary from(DiningTable table) {
        return new TableSummary(table.id(), table.sectorId(), table.label(), table.token().value(),
                table.tokenIssuedAt(), table.active());
    }
}
