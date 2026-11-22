package br.com.cardapio.adapter.in.web.report;

import br.com.cardapio.adapter.in.web.report.ReportTestData.Owner;
import br.com.cardapio.adapter.in.web.report.ReportTestData.Place;
import br.com.cardapio.application.port.out.security.AccessTokenIssuer;
import br.com.cardapio.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static br.com.cardapio.adapter.in.web.report.ReportTestData.SAO_PAULO;
import static br.com.cardapio.adapter.in.web.report.ReportTestData.at;
import static br.com.cardapio.adapter.in.web.report.ReportTestData.canceled;
import static br.com.cardapio.adapter.in.web.report.ReportTestData.item;
import static br.com.cardapio.adapter.in.web.report.ReportTestData.json;
import static br.com.cardapio.adapter.in.web.report.ReportTestData.read;
import static org.assertj.core.api.Assertions.assertThat;

/** Vendas completa (R6b): por setor, mesas, tíquete por pessoa, adiantados e cancelamentos por motivo. */
@IntegrationTest
class SalesFullReportControllerTest {

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

    /**
     * Na semana (7 dias): no Salão, R$ 100 (4 pessoas informadas, R$ 30 pagos adiantado) e R$ 60 (um
     * prato de R$ 25 cancelado: "Acabou o ingrediente"); no Deck, R$ 40 com rodízio de 2 pessoas (dois
     * chopes de R$ 15 cancelados: "Cliente desistiu"). O Deck tem uma mesa a mais, sem venda.
     */
    @BeforeEach
    void sakuraWithAWeek() throws Exception {
        data = new ReportTestData(mvc, jdbc, tokenIssuer);
        sakura = data.newRestaurant();
        salao = data.place(sakura, "Salão");
        deck = data.place(sakura, "Deck");
        jdbc.update("""
                INSERT INTO dining_table (id, establishment_id, sector_id, label, public_token, token_issued_at)
                VALUES (?, ?, ?, 'De2', ?, now())
                """, UUID.randomUUID(), sakura.establishmentId(), deck.sector(), UUID.randomUUID().toString());
        UUID prato = data.product(sakura, "Prato da casa", 2500);
        UUID chopp = data.product(sakura, "Chopp 500 ml", 1500);

        UUID big = data.closedTab(sakura, salao, at(day, 20, 0), at(day, 22, 0), 10000, "PIX", 0,
                item(prato, "Prato da casa", 2500, 4));
        jdbc.update("UPDATE tab SET people = 4 WHERE id = ?", big);
        jdbc.update("""
                INSERT INTO tab_payment (id, establishment_id, tab_id, amount_cents, payment_method, received_at,
                                         received_by)
                VALUES (?, ?, ?, 3000, 'PIX', ?, ?)
                """, UUID.randomUUID(), sakura.establishmentId(), big, Timestamp.from(at(day, 21, 0)), sakura.userId());
        UUID salaoTab = data.closedTab(sakura, salao, at(first, 12, 0), at(first, 13, 0), 6000, "CARD", 0,
                item(chopp, "Chopp 500 ml", 1500, 4), canceled(prato, "Prato da casa", 2500, 1));
        jdbc.update("UPDATE order_item SET cancel_reason = 'Acabou o ingrediente' "
                + "WHERE order_id IN (SELECT id FROM customer_order WHERE tab_id = ?) AND status = 'CANCELED'", salaoTab);
        UUID deckTab = data.closedTab(sakura, deck, at(day, 19, 0), at(day, 20, 0), 4000, "CASH", 0,
                canceled(chopp, "Chopp 500 ml", 1500, 2));
        data.rodizio(sakura, deckTab, at(day, 19, 0), 2);
    }

    private String sales() {
        return "/api/v1/reports/sales?from=%s&to=%s".formatted(first, day);
    }

    @Test
    void theProGetsNoneOfTheBusinessBlocks() throws Exception {
        assertThat(json(data.get(sakura, sales()), "$.full")).isNull();
    }

    @Test
    void bySectorAndTheTablesThatSellTheMost() throws Exception {
        data.onPlan(sakura, "business");

        MvcTestResult result = data.get(sakura, sales());

        List<String> sectors = read(result, "$.full.bySector[*].name");
        assertThat(sectors).containsExactly("Salão", "Deck");
        assertThat(json(result, "$.full.bySector[0].revenueCents")).isEqualTo("16000");
        assertThat(json(result, "$.full.bySector[0].tables")).isEqualTo("1");
        // R$ 160 / 1 mesa / 7 dias; R$ 40 / 2 mesas / 7 dias.
        assertThat(json(result, "$.full.bySector[0].perTablePerDayCents")).isEqualTo("2286");
        assertThat(json(result, "$.full.bySector[1].tables")).isEqualTo("2");
        assertThat(json(result, "$.full.bySector[1].perTablePerDayCents")).isEqualTo("286");
        assertThat(json(result, "$.full.topTables[0].label")).isEqualTo("Sa1");
        assertThat(json(result, "$.full.topTables[0].sector")).isEqualTo("Salão");
        assertThat(json(result, "$.full.topTables[0].tabs")).isEqualTo("2");
        assertThat(json(result, "$.full.topTables[0].averageTicketCents")).isEqualTo("8000");
        assertThat(json(result, "$.full.topTables[1].label")).isEqualTo("De1");
    }

    @Test
    void theTicketPerPersonInAndOutOfTheRodizio() throws Exception {
        data.onPlan(sakura, "business");

        MvcTestResult result = data.get(sakura, sales());

        assertThat(json(result, "$.full.rodizio.people")).isEqualTo("2");
        assertThat(json(result, "$.full.rodizio.averageCents")).isEqualTo("2000");
        assertThat(json(result, "$.full.people.people")).isEqualTo("4");
        assertThat(json(result, "$.full.people.averageCents")).isEqualTo("2500");
        assertThat(json(result, "$.full.comparedPeople.averageCents")).isNull();
    }

    @Test
    void advancePaymentsAndCancellationsByReason() throws Exception {
        data.onPlan(sakura, "business");

        MvcTestResult result = data.get(sakura, sales());

        assertThat(json(result, "$.full.advanceTabs")).isEqualTo("1");
        assertThat(json(result, "$.full.advanceCents")).isEqualTo("3000");
        assertThat(json(result, "$.full.canceledCents")).isEqualTo("5500");
        assertThat(json(result, "$.full.comparedCanceledCents")).isEqualTo("0");
        List<String> reasons = read(result, "$.full.reasons[*].reason");
        assertThat(reasons).containsExactly("Cliente desistiu", "Acabou o ingrediente");
        assertThat(json(result, "$.full.reasons[0].amountCents")).isEqualTo("3000");
    }

    @Test
    void theSectorFilterAndAnotherRestaurant() throws Exception {
        data.onPlan(sakura, "business");
        Owner other = data.newRestaurant();
        data.onPlan(other, "business");
        Place theirs = data.place(other, "Varanda");
        data.closedTab(other, theirs, at(day, 20, 0), at(day, 21, 0), 99000, "PIX", 0);

        MvcTestResult deckOnly = data.get(sakura, sales() + "&sectorId=" + deck.sector());
        MvcTestResult mine = data.get(sakura, sales());
        MvcTestResult theirsReport = data.get(other, sales());

        assertThat(read(deckOnly, "$.full.bySector[*].name").toString()).isEqualTo("[\"Deck\"]");
        assertThat(read(deckOnly, "$.full.topTables[*].label").toString()).isEqualTo("[\"De1\"]");
        assertThat(json(deckOnly, "$.full.advanceTabs")).isEqualTo("0");
        assertThat(read(mine, "$.full.bySector[*].name").toString()).doesNotContain("Varanda");
        assertThat(read(mine, "$.full.topTables[*].label").toString()).doesNotContain("Va1");
        assertThat(read(theirsReport, "$.full.bySector[*].name").toString()).isEqualTo("[\"Varanda\"]");
        assertThat(json(theirsReport, "$.full.canceledCents")).isEqualTo("0");
    }
}
