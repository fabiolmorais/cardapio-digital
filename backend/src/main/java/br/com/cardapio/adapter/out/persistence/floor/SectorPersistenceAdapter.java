package br.com.cardapio.adapter.out.persistence.floor;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentFilter;
import br.com.cardapio.application.port.out.floor.SectorOverview;
import br.com.cardapio.application.port.out.floor.SectorPort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.exception.SectorNameTakenException;
import br.com.cardapio.domain.floor.model.Sector;
import br.com.cardapio.domain.floor.model.SectorId;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
class SectorPersistenceAdapter implements SectorPort {

    /** Um id que nunca existe, para "não excluir ninguém" nas consultas de nome repetido. */
    static final UUID NONE = new UUID(0, 0);

    private final SectorJpaRepository sectors;
    private final DiningTableJpaRepository tables;
    private final EstablishmentFilter establishmentFilter;

    SectorPersistenceAdapter(SectorJpaRepository sectors, DiningTableJpaRepository tables,
                             EstablishmentFilter establishmentFilter) {
        this.sectors = sectors;
        this.tables = tables;
        this.establishmentFilter = establishmentFilter;
    }

    /**
     * saveAndFlush: o INSERT/UPDATE vai ao banco agora, dentro deste try. Se dois pedidos
     * passarem juntos pela checagem do caso de uso, o índice único barra o segundo, e o erro
     * do banco vira o mesmo 409 da checagem.
     */
    @Override
    public void save(Sector sector) {
        establishmentFilter.enable(sector.establishmentId());
        try {
            sectors.saveAndFlush(FloorPersistenceMapper.toEntity(sector));
        } catch (DataIntegrityViolationException e) {
            throw new SectorNameTakenException(sector.name());
        }
    }

    @Override
    public Optional<Sector> find(EstablishmentId establishmentId, SectorId id) {
        establishmentFilter.enable(establishmentId);
        return sectors.findByIdAndEstablishmentId(id.value(), establishmentId.value())
                .map(FloorPersistenceMapper::toDomain);
    }

    @Override
    public ResultPage<SectorOverview> findActive(EstablishmentId establishmentId, PageQuery page) {
        establishmentFilter.enable(establishmentId);
        Page<SectorRow> rows = sectors.findActive(establishmentId.value(), PageRequest.of(page.page(), page.size()));
        return new ResultPage<>(rows.map(FloorPersistenceMapper::toDomain).getContent(),
                page.page(), page.size(), rows.getTotalElements());
    }

    @Override
    public boolean activeNameExists(EstablishmentId establishmentId, String name, SectorId exceptId) {
        establishmentFilter.enable(establishmentId);
        return sectors.activeNameExists(establishmentId.value(), name, exceptId == null ? NONE : exceptId.value());
    }

    @Override
    public int nextPosition(EstablishmentId establishmentId) {
        establishmentFilter.enable(establishmentId);
        return sectors.nextPosition(establishmentId.value());
    }

    @Override
    public long countActiveTables(EstablishmentId establishmentId, SectorId sectorId) {
        establishmentFilter.enable(establishmentId);
        return tables.countActiveBySector(establishmentId.value(), sectorId.value());
    }
}
