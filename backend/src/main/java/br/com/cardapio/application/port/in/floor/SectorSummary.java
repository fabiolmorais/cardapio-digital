package br.com.cardapio.application.port.in.floor;

import br.com.cardapio.application.port.out.floor.SectorOverview;
import br.com.cardapio.domain.floor.model.SectorId;

public record SectorSummary(SectorId id, String name, int position, long activeTables) {

    public static SectorSummary from(SectorOverview overview) {
        return new SectorSummary(overview.sector().id(), overview.sector().name(), overview.sector().position(),
                overview.activeTables());
    }
}
