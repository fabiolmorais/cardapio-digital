package br.com.cardapio.application.service.floor;

import br.com.cardapio.application.port.in.floor.CreateTableCommand;
import br.com.cardapio.application.port.in.floor.TableSummary;
import br.com.cardapio.application.port.in.floor.UpdateTableCommand;
import br.com.cardapio.application.port.out.report.ReportSummaryPort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.exception.SectorInactiveException;
import br.com.cardapio.domain.floor.exception.SectorNotFoundException;
import br.com.cardapio.domain.floor.exception.TableLabelTakenException;
import br.com.cardapio.domain.floor.exception.TableNotFoundException;
import br.com.cardapio.domain.floor.model.DiningTable;
import br.com.cardapio.domain.floor.model.Sector;
import br.com.cardapio.domain.floor.model.SectorId;
import br.com.cardapio.domain.plan.exception.PlanLimitReachedException;
import br.com.cardapio.domain.plan.model.PlanUsage;
import br.com.cardapio.domain.report.model.ItemSales;
import br.com.cardapio.domain.report.model.PrepTotal;
import br.com.cardapio.domain.report.model.SummaryStatus;
import br.com.cardapio.support.InMemorySubscriptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TableServicesTest {

    private final FloorFakes fakes = new FloorFakes();
    private final InMemorySubscriptions subscriptions = new InMemorySubscriptions();
    private final CreateTableService create = new CreateTableService(fakes, fakes, fakes, subscriptions, fakes.clock());
    private final RecordingSummary summary = new RecordingSummary();
    private final UpdateTableService update = new UpdateTableService(fakes, fakes, subscriptions, summary);
    private final RotateTableTokenService rotate = new RotateTableTokenService(fakes, fakes, fakes.clock());
    private final ListTablesService list = new ListTablesService(fakes, (establishment, ids) -> java.util.Map.of());
    private final EstablishmentId sakura = EstablishmentId.newId();
    private Sector salao;
    private Sector deck;

    @BeforeEach
    void twoSectors() {
        salao = fakes.givenSector(sakura, "Salão");
        deck = fakes.givenSector(sakura, "Deck");
    }

    private TableSummary createTable(Sector sector, String label) {
        return create.create(new CreateTableCommand(sakura, sector.id(), label));
    }

    @Test
    void newTableComesWithItsOwnToken() {
        TableSummary t04 = createTable(salao, "04");
        TableSummary t05 = createTable(salao, "05");

        assertThat(t04.publicToken()).isNotEqualTo(t05.publicToken());
        assertThat(t04.tokenIssuedAt()).isEqualTo(FloorFakes.NOW);
        assertThat(t04.active()).isTrue();
    }

    @Test
    void labelsAreUniqueInTheRestaurantEvenAcrossSectors() {
        createTable(salao, "B-1");

        assertThatThrownBy(() -> createTable(deck, "b-1")).isInstanceOf(TableLabelTakenException.class);
    }

    @Test
    void listsTwoBeforeTenAndCanFilterBySector() {
        createTable(salao, "10");
        createTable(salao, "2");
        createTable(deck, "1");

        assertThat(list.list(sakura, null, new PageQuery(0, 20)).content())
                .extracting(TableSummary::label).containsExactly("1", "2", "10");
        assertThat(list.list(sakura, salao.id(), new PageQuery(0, 20)).content())
                .extracting(TableSummary::label).containsExactly("2", "10");
    }

    @Test
    void updateRenamesMovesAndDeactivates() {
        TableSummary table = createTable(salao, "04");

        TableSummary changed = update.update(new UpdateTableCommand(sakura, table.id(), "D4", deck.id(), false));

        assertThat(changed.label()).isEqualTo("D4");
        assertThat(changed.sectorId()).isEqualTo(deck.id());
        assertThat(changed.active()).isFalse();
        assertThat(changed.publicToken()).isEqualTo(table.publicToken()); // o QR não muda
        // A mesa mudou de setor: o resumo dos relatórios do restaurante é refeito (ADR 0014).
        assertThat(summary.invalidated).containsExactly(sakura);

        // Só o nome, ou o mesmo setor: o resumo continua valendo.
        update.update(new UpdateTableCommand(sakura, table.id(), "D5", deck.id(), null));
        assertThat(summary.invalidated).containsExactly(sakura);
    }

    @Test
    void cannotMoveToAnotherRestaurantsSectorNorToAnInactiveOne() {
        TableSummary table = createTable(salao, "04");
        Sector elsewhere = fakes.givenSector(EstablishmentId.newId(), "Deles");
        fakes.save(deck.deactivate());

        assertThatThrownBy(() -> update.update(new UpdateTableCommand(sakura, table.id(), null, elsewhere.id(), null)))
                .isInstanceOf(SectorNotFoundException.class);
        // Setor desativado: a mensagem diz qual e o que fazer (antes, "Setor não encontrado").
        assertThatThrownBy(() -> update.update(new UpdateTableCommand(sakura, table.id(), null, deck.id(), null)))
                .isInstanceOf(SectorInactiveException.class).hasMessageContaining("Deck");
        assertThatThrownBy(() -> createTable(elsewhere, "99")).isInstanceOf(SectorNotFoundException.class);
    }

    @Test
    void rotatingTheTokenInvalidatesTheOldQr() {
        TableSummary table = createTable(salao, "04");

        TableSummary rotated = rotate.rotate(sakura, table.id());

        assertThat(rotated.publicToken()).isNotEqualTo(table.publicToken());
        DiningTable stored = fakes.tables.get(table.id());
        assertThat(stored.token().value()).isEqualTo(rotated.publicToken());
    }

    @Test
    void tableOfAnotherRestaurantIsNotFound() {
        Sector theirs = fakes.givenSector(EstablishmentId.newId(), "Salão deles");
        DiningTable elsewhere = fakes.givenTable(theirs, "01");

        assertThatThrownBy(() -> rotate.rotate(sakura, elsewhere.id())).isInstanceOf(TableNotFoundException.class);
        assertThatThrownBy(() -> update.update(new UpdateTableCommand(sakura, elsewhere.id(), "X", null, null)))
                .isInstanceOf(TableNotFoundException.class);
    }

    @Test
    void theEssentialPlanStopsAtTwentyActiveTables() {
        subscriptions.given(InMemorySubscriptions.ESSENTIAL, new PlanUsage(20, 1, 1));

        assertThatThrownBy(() -> createTable(salao, "21"))
                .isInstanceOf(PlanLimitReachedException.class)
                .hasMessage("O plano Essencial permite até 20 mesas ativas. Para ter mais, mude de plano.");
        assertThat(subscriptions.locks).isEqualTo(1); // travou a assinatura antes de contar
    }

    @Test
    void reactivatingATableTakesASeatButDeactivatingNeverIsBlocked() {
        TableSummary table = createTable(salao, "04");
        update.update(new UpdateTableCommand(sakura, table.id(), null, null, false));
        subscriptions.given(InMemorySubscriptions.ESSENTIAL, new PlanUsage(20, 1, 1));

        assertThatThrownBy(() -> update.update(new UpdateTableCommand(sakura, table.id(), null, null, true)))
                .isInstanceOf(PlanLimitReachedException.class);
        // Renomear uma mesa inativa não ocupa vaga.
        assertThat(update.update(new UpdateTableCommand(sakura, table.id(), "05", null, null)).label()).isEqualTo("05");
    }

    /** O resumo dos relatórios: só guarda quais restaurantes foram invalidados. */
    private static final class RecordingSummary implements ReportSummaryPort {
        final List<EstablishmentId> invalidated = new ArrayList<>();

        @Override
        public Optional<LocalDate> summarizedUntil(EstablishmentId establishmentId) {
            return Optional.empty();
        }

        @Override
        public List<ItemSales> sales(EstablishmentId establishmentId, LocalDate from, LocalDate until,
                                     Optional<SectorId> sector) {
            return List.of();
        }

        @Override
        public List<PrepTotal> preparations(EstablishmentId establishmentId, LocalDate from, LocalDate until,
                                            Optional<SectorId> sector) {
            return List.of();
        }

        @Override
        public List<SummaryStatus> statuses() {
            return List.of();
        }

        @Override
        public void rebuild(EstablishmentId establishmentId, ZoneId zone, LocalDate from, LocalDate until,
                            Instant now) {
        }

        @Override
        public void invalidate(EstablishmentId establishmentId) {
            invalidated.add(establishmentId);
        }
    }
}
