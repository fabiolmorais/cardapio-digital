package br.com.cardapio.adapter.out.persistence.floor;

import br.com.cardapio.application.port.out.floor.SectorOverview;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.DiningTable;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.floor.model.Sector;
import br.com.cardapio.domain.floor.model.SectorId;
import br.com.cardapio.domain.floor.model.TableToken;

final class FloorPersistenceMapper {

    private FloorPersistenceMapper() {
    }

    static SectorJpaEntity toEntity(Sector s) {
        return new SectorJpaEntity(s.id().value(), s.establishmentId().value(), s.name(), s.position(), s.active());
    }

    static Sector toDomain(SectorJpaEntity e) {
        return new Sector(new SectorId(e.getId()), new EstablishmentId(e.getEstablishmentId()), e.getName(),
                e.getPosition(), e.isActive());
    }

    static SectorOverview toDomain(SectorRow row) {
        Sector sector = new Sector(new SectorId(row.getId()), new EstablishmentId(row.getEstablishmentId()),
                row.getName(), row.getPosition(), row.getActive());
        return new SectorOverview(sector, row.getActiveTables());
    }

    static DiningTableJpaEntity toEntity(DiningTable t) {
        return new DiningTableJpaEntity(t.id().value(), t.establishmentId().value(), t.sectorId().value(), t.label(),
                t.token().value(), t.tokenIssuedAt(), t.active());
    }

    static DiningTable toDomain(DiningTableJpaEntity e) {
        return new DiningTable(new DiningTableId(e.getId()), new EstablishmentId(e.getEstablishmentId()),
                new SectorId(e.getSectorId()), e.getLabel(), new TableToken(e.getPublicToken()),
                e.getTokenIssuedAt(), e.isActive());
    }
}
