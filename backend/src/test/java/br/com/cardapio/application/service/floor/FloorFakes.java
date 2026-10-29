package br.com.cardapio.application.service.floor;

import br.com.cardapio.application.port.out.floor.DiningTablePort;
import br.com.cardapio.application.port.out.floor.PublicTableView;
import br.com.cardapio.application.port.out.floor.SectorOverview;
import br.com.cardapio.application.port.out.floor.SectorPort;
import br.com.cardapio.application.port.out.security.RandomTokenGenerator;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Setores e mesas em memória, com relógio fixo e tokens previsíveis. */
final class FloorFakes implements SectorPort, DiningTablePort, RandomTokenGenerator {

    static final Instant NOW = Instant.parse("2026-10-28T12:00:00Z");

    final Map<SectorId, Sector> sectors = new LinkedHashMap<>();
    final Map<DiningTableId, DiningTable> tables = new LinkedHashMap<>();
    private int tokenCounter;

    Clock clock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }

    Sector givenSector(EstablishmentId establishmentId, String name) {
        Sector sector = Sector.create(establishmentId, name, nextPosition(establishmentId));
        sectors.put(sector.id(), sector);
        return sector;
    }

    DiningTable givenTable(Sector sector, String label) {
        DiningTable table = DiningTable.create(sector.establishmentId(), sector.id(), label,
                new TableToken(generate()), NOW);
        tables.put(table.id(), table);
        return table;
    }

    // SectorPort
    @Override
    public void save(Sector sector) { sectors.put(sector.id(), sector); }

    @Override
    public Optional<Sector> find(EstablishmentId establishmentId, SectorId id) {
        return Optional.ofNullable(sectors.get(id)).filter(s -> s.establishmentId().equals(establishmentId));
    }

    @Override
    public ResultPage<SectorOverview> findActive(EstablishmentId establishmentId, PageQuery page) {
        List<SectorOverview> all = sectors.values().stream()
                .filter(s -> s.establishmentId().equals(establishmentId) && s.active())
                .sorted(Comparator.comparingInt(Sector::position))
                .map(s -> new SectorOverview(s, countActiveTables(establishmentId, s.id())))
                .toList();
        return new ResultPage<>(all, page.page(), page.size(), all.size());
    }

    @Override
    public boolean activeNameExists(EstablishmentId establishmentId, String name, SectorId exceptId) {
        return sectors.values().stream().anyMatch(s -> s.establishmentId().equals(establishmentId) && s.active()
                && s.name().equalsIgnoreCase(name) && !s.id().equals(exceptId));
    }

    @Override
    public int nextPosition(EstablishmentId establishmentId) {
        return (int) sectors.values().stream().filter(s -> s.establishmentId().equals(establishmentId)).count();
    }

    @Override
    public long countActiveTables(EstablishmentId establishmentId, SectorId sectorId) {
        return tables.values().stream().filter(t -> t.sectorId().equals(sectorId) && t.active()).count();
    }

    // DiningTablePort
    @Override
    public void save(DiningTable table) { tables.put(table.id(), table); }

    @Override
    public Optional<DiningTable> find(EstablishmentId establishmentId, DiningTableId id) {
        return Optional.ofNullable(tables.get(id)).filter(t -> t.establishmentId().equals(establishmentId));
    }

    @Override
    public ResultPage<DiningTable> findAll(EstablishmentId establishmentId, SectorId sectorId, PageQuery page) {
        List<DiningTable> all = tables.values().stream()
                .filter(t -> t.establishmentId().equals(establishmentId))
                .filter(t -> sectorId == null || t.sectorId().equals(sectorId))
                .sorted(Comparator.comparing((DiningTable t) -> !t.active())
                        .thenComparingInt(t -> t.label().length()).thenComparing(DiningTable::label))
                .toList();
        return new ResultPage<>(all, page.page(), page.size(), all.size());
    }

    @Override
    public Optional<PublicTableView> findActiveByToken(TableToken token) {
        return tables.values().stream().filter(t -> t.active() && t.token().equals(token)).findFirst()
                .map(t -> new PublicTableView(t, "Sakura Sushi"));
    }

    @Override
    public boolean labelExists(EstablishmentId establishmentId, String label, DiningTableId exceptId) {
        return tables.values().stream().anyMatch(t -> t.establishmentId().equals(establishmentId)
                && t.label().equalsIgnoreCase(label) && !Objects.equals(t.id(), exceptId));
    }

    // RandomTokenGenerator: "token-da-mesa-000000000001", "…002"…
    @Override
    public String generate() { return String.format("token-da-mesa-%012d", ++tokenCounter); }
}
