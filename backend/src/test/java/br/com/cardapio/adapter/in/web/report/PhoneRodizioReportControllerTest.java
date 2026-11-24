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
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static br.com.cardapio.adapter.in.web.report.ReportTestData.SAO_PAULO;
import static br.com.cardapio.adapter.in.web.report.ReportTestData.at;
import static br.com.cardapio.adapter.in.web.report.ReportTestData.json;
import static br.com.cardapio.adapter.in.web.report.ReportTestData.read;
import static org.assertj.core.api.Assertions.assertThat;

/** Pedido pelo celular e rodízio (R11): quem lança, do QR ao pedido, presença, rodízio e passagem. */
@IntegrationTest
class PhoneRodizioReportControllerTest {

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
    private UUID sashimi;
    private UUID hotRoll;
    private UUID picanha;

    /**
     * No último dia: a comanda A (Sa1, 20h às 21h30, rodízio à la minute de 3 pessoas e tempo limite
     * de 1 h, R$ 359,70) com dois celulares (um pede às 20h04, 6 min depois de escanear; o outro não
     * pede), duas rodadas (4 sashimis e 2 hot rolls pelo celular; 3 sashimis e 1 hot roll cancelado
     * pelo garçom) e uma rodada recusada; a comanda B (De1, 12h às 13h, R$ 50, pedido do garçom, sem
     * celular); a comanda C (De1, 18h às 19h, passagem de 1 pessoa, 6 picanhas, cartão vermelho das
     * 18h20 às 18h30). No primeiro dia, dois celulares sem comanda (um deles bloqueado).
     */
    @BeforeEach
    void sakuraWithPhonesAndRodizio() throws Exception {
        data = new ReportTestData(mvc, jdbc, tokenIssuer);
        sakura = data.newRestaurant();
        salao = data.place(sakura, "Salão");
        deck = data.place(sakura, "Deck");
        sashimi = data.product(sakura, "Sashimi", 2500);
        hotRoll = data.product(sakura, "Hot roll", 2500);
        picanha = data.product(sakura, "Picanha", 2500);

        UUID a = data.closedTab(sakura, salao, at(day, 20, 0), at(day, 21, 30), 35970, "PIX", 0);
        data.rodizio(sakura, a, at(day, 20, 5), 2, 1);
        UUID rodizioA = tabRodizio(a);
        jdbc.update("UPDATE tab_rodizio SET ends_at = ? WHERE id = ?", Timestamp.from(at(day, 21, 5)), rodizioA);
        UUID s1 = session(salao, at(day, 19, 58), at(day, 21, 0), "WIFI");
        session(salao, at(day, 20, 10), at(day, 20, 40), "GPS");
        UUID o1 = order(a, salao, s1, null, "SENT", at(day, 20, 4));
        item(o1, rodizioA, sashimi, "Sashimi", 4, "DELIVERED");
        item(o1, rodizioA, hotRoll, "Hot roll", 2, "DELIVERED");
        UUID o2 = order(a, salao, null, sakura.userId(), "SENT", at(day, 20, 30));
        item(o2, rodizioA, sashimi, "Sashimi", 3, "DELIVERED");
        item(o2, rodizioA, hotRoll, "Hot roll", 1, "CANCELED");
        UUID rejected = order(a, salao, s1, null, "REJECTED", at(day, 20, 50));
        item(rejected, rodizioA, sashimi, "Sashimi", 5, "CANCELED");

        UUID b = data.closedTab(sakura, deck, at(day, 12, 0), at(day, 13, 0), 5000, "PIX", 0);
        order(b, deck, null, sakura.userId(), "SENT", at(day, 12, 5));

        UUID c = data.closedTab(sakura, deck, at(day, 18, 0), at(day, 19, 0), 9990, "PIX", 0);
        data.rodizio(sakura, c, at(day, 18, 0), 1);
        UUID rodizioC = tabRodizio(c);
        jdbc.update("UPDATE tab_rodizio SET rodizio_type = 'PASSAGEM' WHERE id = ?", rodizioC);
        card(rodizioC, "PAUSED", at(day, 18, 20));
        card(rodizioC, "SERVING", at(day, 18, 30));
        UUID o5 = order(c, deck, null, sakura.userId(), "SENT", at(day, 18, 10));
        item(o5, rodizioC, picanha, "Picanha", 6, "DELIVERED");

        session(deck, at(first, 15, 0), at(first, 15, 20), "WIFI");
        UUID blocked = session(salao, at(first, 20, 0), at(first, 20, 5), null);
        jdbc.update("UPDATE table_session SET blocked_at = ?, blocked_by = ? WHERE id = ?",
                Timestamp.from(at(first, 20, 5)), sakura.userId(), blocked);
    }

    private UUID tabRodizio(UUID tab) {
        return jdbc.queryForObject("SELECT id FROM tab_rodizio WHERE tab_id = ?", UUID.class, tab);
    }

    private UUID session(Place place, Instant created, Instant lastSeen, String presence) {
        UUID session = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO table_session (id, establishment_id, table_id, token_hash, created_at, last_seen_at,
                                           expires_at, presence_method, presence_confirmed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, session, sakura.establishmentId(), place.table(), UUID.randomUUID().toString().replace("-", ""),
                Timestamp.from(created), Timestamp.from(lastSeen), Timestamp.from(lastSeen.plus(Duration.ofHours(2))),
                presence, presence == null ? null : Timestamp.from(created));
        return session;
    }

    private UUID order(UUID tab, Place place, UUID session, UUID placedBy, String status, Instant at) {
        UUID order = UUID.randomUUID();
        Integer number = jdbc.queryForObject("SELECT count(*) + 1 FROM customer_order WHERE tab_id = ?",
                Integer.class, tab);
        jdbc.update("""
                INSERT INTO customer_order (id, establishment_id, tab_id, table_id, session_id, placed_by, number, status,
                                            created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, order, sakura.establishmentId(), tab, place.table(), session, placedBy, number, status,
                Timestamp.from(at));
        return order;
    }

    private void item(UUID order, UUID tabRodizio, UUID product, String name, int quantity, String status) {
        Integer position = jdbc.queryForObject("SELECT count(*) FROM order_item WHERE order_id = ?", Integer.class,
                order);
        Timestamp created = jdbc.queryForObject("SELECT created_at FROM customer_order WHERE id = ?",
                Timestamp.class, order);
        jdbc.update("""
                INSERT INTO order_item (id, establishment_id, order_id, position, product_id, product_name,
                                        unit_price_cents, quantity, status, created_at, tab_rodizio_id, canceled_at,
                                        canceled_by)
                VALUES (?, ?, ?, ?, ?, ?, 0, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), sakura.establishmentId(), order, position, product, name, quantity, status,
                created, tabRodizio, "CANCELED".equals(status) ? created : null,
                "CANCELED".equals(status) ? sakura.userId() : null);
    }

    private void card(UUID tabRodizio, String status, Instant at) {
        jdbc.update("""
                INSERT INTO rodizio_card_change (id, establishment_id, tab_rodizio_id, status, changed_at)
                VALUES (?, ?, ?, ?, ?)
                """, UUID.randomUUID(), sakura.establishmentId(), tabRodizio, status, Timestamp.from(at));
    }

    private String report() {
        return "/api/v1/reports/phone-and-rodizio?from=%s&to=%s".formatted(first, day);
    }

    @Test
    void theProAndTheStaffCannotSeeIt() {
        assertThat(data.get(sakura, report())).hasStatus(HttpStatus.FORBIDDEN);
        data.onPlan(sakura, "business");
        assertThat(data.get(data.token(sakura, Role.STAFF), report())).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(data.get(data.token(sakura, Role.MANAGER), report())).hasStatusOk();
    }

    @Test
    void whoPlacesTheOrders() throws Exception {
        data.onPlan(sakura, "business");

        MvcTestResult result = data.get(sakura, report());

        // 7 dias: um ponto por dia. No último, 1 pedido pelo celular e 3 da equipe (o recusado não conta).
        assertThat(json(result, "$.grouping")).isEqualTo("DAY");
        List<String> starts = read(result, "$.origin[*].start");
        assertThat(starts).hasSize(7).startsWith(first.toString()).endsWith(day.toString());
        assertThat(json(result, "$.origin[6].phone")).isEqualTo("1");
        assertThat(json(result, "$.origin[6].staff")).isEqualTo("3");
        assertThat(json(result, "$.origin[0].phone")).isEqualTo("0");
    }

    @Test
    void fromTheQrToTheOrder() throws Exception {
        data.onPlan(sakura, "business");

        MvcTestResult result = data.get(sakura, report());

        assertThat(json(result, "$.funnel.tabs")).isEqualTo("3");
        assertThat(json(result, "$.funnel.scanned")).isEqualTo("1");
        assertThat(json(result, "$.funnel.phoneOrdered")).isEqualTo("1");
        assertThat(json(result, "$.phones.phonesPerTab")).isEqualTo("2.0");
        assertThat(json(result, "$.phones.orderingPerTab")).isEqualTo("1.0");
        assertThat(json(result, "$.phones.scanToOrderSeconds")).isEqualTo("360");
        assertThat(json(result, "$.phones.scannedNoOrder")).isEqualTo("2");
        assertThat(json(result, "$.phones.rejected")).isEqualTo("1");
        assertThat(json(result, "$.phones.blocked")).isEqualTo("1");
        assertThat(json(result, "$.presence[0].method")).isEqualTo("WIFI");
        assertThat(json(result, "$.presence[0].sessions")).isEqualTo("2");
        assertThat(json(result, "$.presence[1].method")).isEqualTo("GPS");
    }

    @Test
    void theRodizio() throws Exception {
        data.onPlan(sakura, "business");

        MvcTestResult result = data.get(sakura, report());

        assertThat(json(result, "$.rodizio.tabs")).isEqualTo("2");
        assertThat(json(result, "$.rodizio.people")).isEqualTo("4");
        assertThat(json(result, "$.rodizio.revenueCents")).isEqualTo("39960");
        assertThat(json(result, "$.rodizio.extraCents")).isEqualTo("6000");
        assertThat(json(result, "$.rodizio.rounds")).isEqualTo("3");
        assertThat(json(result, "$.rodizio.items")).isEqualTo("15");
        assertThat(json(result, "$.rodizio.timeSeconds")).isEqualTo("4350");
        assertThat(json(result, "$.rodizio.withLimit")).isEqualTo("1");
        assertThat(json(result, "$.rodizio.reachedLimit")).isEqualTo("1");
        assertThat(json(result, "$.tiers[0].tier")).isEqualTo("Faixa 0");
        assertThat(json(result, "$.tiers[0].people")).isEqualTo("3");
        List<String> items = read(result, "$.topItems[*].name");
        assertThat(items).containsExactly("Sashimi", "Picanha", "Hot roll");
        assertThat(json(result, "$.topItems[0].quantity")).isEqualTo("7");
    }

    @Test
    void thePassagemCard() throws Exception {
        data.onPlan(sakura, "business");

        MvcTestResult result = data.get(sakura, report());

        assertThat(json(result, "$.card.servingSeconds")).isEqualTo("3000");
        assertThat(json(result, "$.card.pausedSeconds")).isEqualTo("600");
        List<String> cuts = read(result, "$.topCuts[*].name");
        assertThat(cuts).containsExactly("Picanha");
    }

    @Test
    void theSectorFilter() throws Exception {
        data.onPlan(sakura, "business");

        MvcTestResult result = data.get(sakura, report() + "&sectorId=" + deck.sector());

        assertThat(json(result, "$.funnel.tabs")).isEqualTo("2");
        assertThat(json(result, "$.funnel.scanned")).isEqualTo("0");
        assertThat(json(result, "$.phones.scannedNoOrder")).isEqualTo("1");
        assertThat(json(result, "$.phones.blocked")).isEqualTo("0");
        assertThat(json(result, "$.rodizio.tabs")).isEqualTo("1");
        assertThat(json(result, "$.rodizio.people")).isEqualTo("1");
    }

    @Test
    void anotherRestaurantNeverShowsUp() throws Exception {
        data.onPlan(sakura, "business");
        Owner other = data.newRestaurant();
        data.onPlan(other, "business");
        Place theirs = data.place(other, "Varanda");
        UUID tab = data.closedTab(other, theirs, at(day, 20, 0), at(day, 22, 0), 9000, "PIX", 0);
        UUID phone = data.session(other, theirs, at(day, 20, 0));
        data.phoneOrder(other, theirs, tab, phone, at(day, 20, 5));

        MvcTestResult mine = data.get(sakura, report());
        MvcTestResult theirsReport = data.get(other, report());

        assertThat(json(mine, "$.funnel.tabs")).isEqualTo("3");
        assertThat(json(mine, "$.origin[6].phone")).isEqualTo("1");
        assertThat(json(theirsReport, "$.funnel.tabs")).isEqualTo("1");
        assertThat(json(theirsReport, "$.funnel.phoneOrdered")).isEqualTo("1");
        assertThat(json(theirsReport, "$.rodizio.tabs")).isEqualTo("0");
        assertThat(json(theirsReport, "$.phones.rejected")).isEqualTo("0");
        assertThat(data.get(sakura, report() + "&sectorId=" + theirs.sector())).hasStatus(HttpStatus.NOT_FOUND);
    }
}
