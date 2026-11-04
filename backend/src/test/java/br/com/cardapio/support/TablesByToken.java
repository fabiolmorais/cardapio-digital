package br.com.cardapio.support;

import br.com.cardapio.application.port.out.floor.DiningTablePort;
import br.com.cardapio.application.port.out.floor.PublicTableView;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.DiningTable;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.floor.model.SectorId;
import br.com.cardapio.domain.floor.model.TableToken;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * As mesas pelo token do QR (e pelo id), para os testes das telas públicas e do rodízio, que
 * não precisam do resto do salão. O que elas não usam lança UnsupportedOperationException.
 */
public final class TablesByToken implements DiningTablePort {

    private final Map<TableToken, DiningTable> tables = new HashMap<>();

    /** Uma mesa ativa do restaurante, com um token conhecido. */
    public DiningTable given(EstablishmentId establishmentId, String label, String token) {
        DiningTable table = DiningTable.create(establishmentId, SectorId.newId(), label, new TableToken(token),
                Instant.parse("2026-11-02T12:00:00Z"));
        tables.put(table.token(), table);
        return table;
    }

    @Override
    public Optional<PublicTableView> findActiveByToken(TableToken token) {
        return Optional.ofNullable(tables.get(token)).map(t -> new PublicTableView(t, "Restaurante"));
    }

    @Override
    public void save(DiningTable table) { throw new UnsupportedOperationException(); }

    @Override
    public Optional<DiningTable> find(EstablishmentId establishmentId, DiningTableId id) {
        return tables.values().stream()
                .filter(t -> t.id().equals(id) && t.establishmentId().equals(establishmentId))
                .findFirst();
    }

    @Override
    public ResultPage<DiningTable> findAll(EstablishmentId establishmentId, SectorId sectorId, PageQuery page) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean labelExists(EstablishmentId establishmentId, String label, DiningTableId exceptId) {
        throw new UnsupportedOperationException();
    }
}
