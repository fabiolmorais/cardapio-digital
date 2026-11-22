package br.com.cardapio.adapter.in.web.report;

import br.com.cardapio.adapter.in.web.report.ReportTestData.Owner;
import br.com.cardapio.adapter.in.web.report.ReportTestData.Place;
import br.com.cardapio.application.port.out.security.AccessTokenIssuer;
import br.com.cardapio.domain.account.model.Role;
import br.com.cardapio.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static br.com.cardapio.adapter.in.web.report.ReportTestData.SAO_PAULO;
import static br.com.cardapio.adapter.in.web.report.ReportTestData.at;
import static br.com.cardapio.adapter.in.web.report.ReportTestData.item;
import static br.com.cardapio.adapter.in.web.report.ReportTestData.json;
import static br.com.cardapio.adapter.in.web.report.ReportTestData.read;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** Mesas e ocupação (R10): permanência, giro, mapa do salão, ocupação, esquecidas e mudanças de mesa. */
@IntegrationTest
class TableReportControllerTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    AccessTokenIssuer tokenIssuer;

    @Autowired
    JdbcTemplate jdbc;

    private final LocalDate day = LocalDate.now(SAO_PAULO).minusDays(2);
    private final LocalDate first = day.minusDays(6);

    private ReportTestData data;
    private Owner sakura;
    private Place salao;
    private Place deck;
    private UUID dinner;

    /**
     * Duas mesas (Sa1 e De1) e cinco comandas fechadas nos 7 dias: no último dia, Sa1 das 12h às 13h
     * (R$ 25) e das 20h às 21h30 (R$ 50, com rodízio), De1 das 20h às 21h (R$ 30); no primeiro, Sa1 das
     * 12h às 13h (R$ 25) e De1 das 23h às 10h do dia seguinte (R$ 10, esquecida). Na Sa1, uma comanda
     * aberta desde as 23h do último dia (esquecida, ainda aberta). Duas mudanças saindo da Sa1 e uma
     * junção.
     */
    @BeforeEach
    void sakuraWithTwoTables() throws Exception {
        data = new ReportTestData(mvc, jdbc, tokenIssuer);
        sakura = data.newRestaurant();
        salao = data.place(sakura, "Salão");
        deck = data.place(sakura, "Deck");
        UUID prato = data.product(sakura, "Prato da casa", 2500);

        dinner = data.closedTab(sakura, salao, at(day, 20, 0), at(day, 21, 30), 5000, "PIX", 0,
                item(prato, "Prato da casa", 2500, 2));
        data.rodizio(sakura, dinner, at(day, 20, 5), 2);
        UUID lunch = data.closedTab(sakura, salao, at(day, 12, 0), at(day, 13, 0), 2500, "PIX", 0,
                item(prato, "Prato da casa", 2500, 1));
        UUID deckDinner = data.closedTab(sakura, deck, at(day, 20, 0), at(day, 21, 0), 3000, "PIX", 0);
        data.closedTab(sakura, salao, at(first, 12, 0), at(first, 13, 0), 2500, "PIX", 0);
        data.closedTab(sakura, deck, at(first, 23, 0), at(first.plusDays(1), 10, 0), 1000, "PIX", 0);
        data.openTab(sakura, salao, at(day, 23, 0));

        move(dinner, salao, deck, at(day, 20, 30), null);
        move(lunch, salao, deck, at(day, 12, 30), null);
        move(deckDinner, deck, salao, at(day, 20, 45), dinner);
    }

    private void move(UUID tab, Place from, Place to, Instant at, UUID mergedInto) {
        jdbc.update("""
                INSERT INTO tab_move (id, establishment_id, tab_id, from_table_id, to_table_id, merged_tab_id, moved_by,
                                      moved_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), sakura.establishmentId(), tab, from.table(), to.table(), mergedInto,
                sakura.userId(), Timestamp.from(at));
    }

    private String tables() {
        return "/api/v1/reports/tables?from=%s&to=%s".formatted(first, day);
    }

    @Test
    void theProAndTheStaffCannotSeeIt() {
        assertThat(data.get(sakura, tables())).hasStatus(HttpStatus.FORBIDDEN);
        data.onPlan(sakura, "business");
        assertThat(data.get(data.token(sakura, Role.STAFF), tables())).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(data.get(data.token(sakura, Role.MANAGER), tables())).hasStatusOk();
    }

    @Test
    void theStayTheTurnoverAndThePerHour() throws Exception {
        data.onPlan(sakura, "business");

        MvcTestResult result = data.get(sakura, tables());

        // 5 comandas: 1h30 + 1h + 1h + 1h + 11h = 15h30 de mesa (55.800 s), R$ 140.
        assertThat(json(result, "$.current.tabs")).isEqualTo("5");
        assertThat(json(result, "$.current.staySeconds")).isEqualTo("11160");
        assertThat(json(result, "$.current.rodizioStaySeconds")).isEqualTo("5400");
        assertThat(json(result, "$.current.aLaCarteStaySeconds")).isEqualTo("12600");
        assertThat(Double.parseDouble(json(result, "$.current.turnover"))).isCloseTo(5.0 / 2 / 7,
                within(0.0001));
        assertThat(json(result, "$.current.perOccupiedHourCents")).isEqualTo("903");
        assertThat(json(result, "$.compared.tabs")).isEqualTo("0");
        assertThat(json(result, "$.compared.staySeconds")).isNull();
        assertThat(json(result, "$.compared.perOccupiedHourCents")).isNull();
    }

    @Test
    void theFloorMapAndTheOccupancy() throws Exception {
        data.onPlan(sakura, "business");

        MvcTestResult result = data.get(sakura, tables());

        List<String> labels = read(result, "$.tables[*].label");
        assertThat(labels).containsExactly("De1", "Sa1");
        assertThat(json(result, "$.tables[1].tabs")).isEqualTo("3");
        assertThat(json(result, "$.tables[1].revenueCents")).isEqualTo("10000");
        assertThat(json(result, "$.tables[1].ticketCents")).isEqualTo("3333");
        // 3 dias com movimento; às 20h do último, as duas mesas ocupadas.
        List<Integer> hours = read(result, "$.occupancyByHour[*].hour");
        assertThat(hours).contains(12, 20, 21, 23);
        int at20 = hours.indexOf(20);
        assertThat(Double.parseDouble(json(result, "$.occupancyByHour[" + at20 + "].share")))
                .isCloseTo(1.0 / 3, within(0.0001));
        assertThat(json(result, "$.peak.weekday")).isEqualTo(day.getDayOfWeek().name());
        assertThat(json(result, "$.peak.hour")).isEqualTo("20");
        assertThat(json(result, "$.peak.share")).isEqualTo("1.0");
    }

    @Test
    void forgottenTabsAndMoves() throws Exception {
        data.onPlan(sakura, "business");

        MvcTestResult result = data.get(sakura, tables());

        assertThat(json(result, "$.forgottenCount")).isEqualTo("2");
        assertThat(json(result, "$.forgotten[0].label")).isEqualTo("Sa1");
        assertThat(json(result, "$.forgotten[0].closedAt")).isNull();
        assertThat(json(result, "$.forgotten[1].label")).isEqualTo("De1");
        assertThat(json(result, "$.forgotten[1].closedAt")).isNotNull();
        assertThat(json(result, "$.moves.moves")).isEqualTo("2");
        assertThat(json(result, "$.moves.merges")).isEqualTo("1");
        assertThat(json(result, "$.moves.topFrom")).isEqualTo("Sa1");
        assertThat(json(result, "$.moves.topFromMoves")).isEqualTo("2");
    }

    @Test
    void theSectorFilter() throws Exception {
        data.onPlan(sakura, "business");

        MvcTestResult result = data.get(sakura, tables() + "&sectorId=" + deck.sector());

        List<String> labels = read(result, "$.tables[*].label");
        assertThat(labels).containsExactly("De1");
        assertThat(json(result, "$.current.tabs")).isEqualTo("2");
        assertThat(json(result, "$.forgottenCount")).isEqualTo("1");
        assertThat(json(result, "$.moves.moves")).isEqualTo("0");
        assertThat(json(result, "$.moves.merges")).isEqualTo("1");
    }

    @Test
    void anotherRestaurantNeverShowsUp() throws Exception {
        data.onPlan(sakura, "business");
        Owner other = data.newRestaurant();
        data.onPlan(other, "business");
        Place theirs = data.place(other, "Varanda");
        data.closedTab(other, theirs, at(day, 20, 0), at(day, 22, 0), 9000, "PIX", 0);

        MvcTestResult mine = data.get(sakura, tables());
        MvcTestResult theirsReport = data.get(other, tables());

        assertThat(read(mine, "$.tables[*].label").toString()).doesNotContain("Va1");
        assertThat(json(mine, "$.current.tabs")).isEqualTo("5");
        assertThat(json(theirsReport, "$.current.tabs")).isEqualTo("1");
        assertThat(json(theirsReport, "$.forgottenCount")).isEqualTo("0");
        assertThat(json(theirsReport, "$.moves.moves")).isEqualTo("0");
        List<String> theirLabels = read(theirsReport, "$.tables[*].label");
        assertThat(theirLabels).containsExactly("Va1");
        assertThat(data.get(sakura, tables() + "&sectorId=" + theirs.sector())).hasStatus(HttpStatus.NOT_FOUND);
    }
}
