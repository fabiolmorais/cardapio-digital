package br.com.cardapio.adapter.out.persistence.floor;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentFilter;
import br.com.cardapio.application.port.out.floor.DiningTablePort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.exception.TableLabelTakenException;
import br.com.cardapio.domain.floor.model.DiningTable;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.floor.model.SectorId;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class DiningTablePersistenceAdapter implements DiningTablePort {

    private final DiningTableJpaRepository tables;
    private final EstablishmentFilter establishmentFilter;

    DiningTablePersistenceAdapter(DiningTableJpaRepository tables, EstablishmentFilter establishmentFilter) {
        this.tables = tables;
        this.establishmentFilter = establishmentFilter;
    }

    /** Mesmo cuidado do setor: o índice único do rótulo é a última palavra. */
    @Override
    public void save(DiningTable table) {
        establishmentFilter.enable(table.establishmentId());
        try {
            tables.saveAndFlush(FloorPersistenceMapper.toEntity(table));
        } catch (DataIntegrityViolationException e) {
            throw new TableLabelTakenException(table.label());
        }
    }

    @Override
    public Optional<DiningTable> find(EstablishmentId establishmentId, DiningTableId id) {
        establishmentFilter.enable(establishmentId);
        return tables.findByIdAndEstablishmentId(id.value(), establishmentId.value())
                .map(FloorPersistenceMapper::toDomain);
    }

    @Override
    public ResultPage<DiningTable> findAll(EstablishmentId establishmentId, SectorId sectorId, PageQuery page) {
        establishmentFilter.enable(establishmentId);
        PageRequest pageable = PageRequest.of(page.page(), page.size());
        Page<DiningTableJpaEntity> rows = sectorId == null
                ? tables.findAll(establishmentId.value(), pageable)
                : tables.findBySector(establishmentId.value(), sectorId.value(), pageable);
        return new ResultPage<>(rows.map(FloorPersistenceMapper::toDomain).getContent(),
                page.page(), page.size(), rows.getTotalElements());
    }

    @Override
    public boolean labelExists(EstablishmentId establishmentId, String label, DiningTableId exceptId) {
        establishmentFilter.enable(establishmentId);
        return tables.labelExists(establishmentId.value(), label,
                exceptId == null ? SectorPersistenceAdapter.NONE : exceptId.value());
    }
}
