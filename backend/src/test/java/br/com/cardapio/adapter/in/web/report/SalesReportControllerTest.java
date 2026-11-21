package br.com.cardapio.adapter.in.web.report;

import br.com.cardapio.application.port.out.security.AccessTokenIssuer;
import br.com.cardapio.application.port.out.security.TokenSubject;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.Role;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.support.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** A Visão geral e Vendas (R1): as somas das comandas fechadas, sempre de um restaurante só. */
@IntegrationTest
class SalesReportControllerTest {

    private static final String PASSWORD = "sushi2026bom";
    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    @Autowired
    MockMvcTester mvc;

    @Autowired
    AccessTokenIssuer tokenIssuer;

    @Autowired
    JdbcTemplate jdbc;

    record Owner(String token, UUID userId, UUID establishmentId) {
    }

    record Place(UUID sector, UUID table) {
    }

    record Item(UUID product, String name, long priceCents, int quantity, boolean canceled) {
    }

    /** O último dia do período dos testes: anteontem, um dia inteiro no passado. */
    private final LocalDate day = LocalDate.now(SAO_PAULO).minusDays(2);

    private Owner sakura;
    private Place salao;
    private UUID prato;
    private UUID chopp;

    @BeforeEach
    void sakuraWithASalon() throws Exception {
        sakura = newRestaurant();
        salao = place(sakura, "Salão");
        prato = product(sakura, "Prato da casa", 2500);
        chopp = product(sakura, "Chopp 500 ml", 1500);
    }

    // ---------- montagem dos dados ----------

    private Owner newRestaurant() throws Exception {
        String email = "dono-" + UUID.randomUUID() + "@teste.com";
        String signup = """
                {"establishmentName":"Restaurante %s","ownerName":"Marina","document":"11.222.333/0001-81",
                 "whatsapp":"(11) 98765-4321","email":"%s","password":"%s","acceptedTerms":true}
                """.formatted(UUID.randomUUID(), email, PASSWORD);
        assertThat(mvc.post().uri("/api/v1/signup").contentType(MediaType.APPLICATION_JSON).content(signup).exchange())
                .hasStatus(HttpStatus.CREATED);
        MvcTestResult login = mvc.post().uri("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)).exchange();
        return new Owner(json(login, "$.accessToken"), UUID.fromString(json(login, "$.userId")),
                UUID.fromString(json(login, "$.establishment.id")));
    }

    private Place place(Owner owner, String sectorName) {
        UUID sector = UUID.randomUUID();
        UUID table = UUID.randomUUID();
        jdbc.update("INSERT INTO sector (id, establishment_id, name, position) VALUES (?, ?, ?, 0)",
                sector, owner.establishmentId(), sectorName);
        jdbc.update("""
                INSERT INTO dining_table (id, establishment_id, sector_id, label, public_token, token_issued_at)
                VALUES (?, ?, ?, ?, ?, now())
                """, table, owner.establishmentId(), sector, sectorName.substring(0, 2) + "1", UUID.randomUUID().toString());
        return new Place(sector, table);
    }

    private UUID product(Owner owner, String name, long priceCents) {
        UUID category = UUID.randomUUID();
        UUID product = UUID.randomUUID();
        jdbc.update("INSERT INTO menu_category (id, establishment_id, name, position) VALUES (?, ?, ?, 0)",
                category, owner.establishmentId(), "Cat " + name.substring(0, 4));
        jdbc.update("""
                INSERT INTO product (id, establishment_id, category_id, name, price_cents, position)
                VALUES (?, ?, ?, ?, ?, 0)
                """, product, owner.establishmentId(), category, name, priceCents);
        return product;
    }

    private static Instant at(LocalDate date, int hour, int minute) {
        return ZonedDateTime.of(date, LocalTime.of(hour, minute), SAO_PAULO).toInstant();
    }

    /** Uma comanda fechada, com os itens, o pagamento (total + serviço) e quem fechou. */
    private UUID closedTab(Owner owner, Place place, Instant opened, Instant closed, long totalCents, String method,
                           long serviceCents, Item... items) {
        UUID tab = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO tab (id, establishment_id, table_id, status, opened_at, closed_at, closed_by, total_cents,
                                 payment_method, service_percent, service_accepted, service_cents, service_destination)
                VALUES (?, ?, ?, 'CLOSED', ?, ?, ?, ?, ?, 10, ?, ?, 'TEAM')
                """, tab, owner.establishmentId(), place.table(), Timestamp.from(opened), Timestamp.from(closed),
                owner.userId(), totalCents, method, serviceCents > 0, serviceCents);
        addItems(owner, place, tab, opened, items);
        if (totalCents + serviceCents == 0) {
            return tab; // conta zerada: o app não grava pagamento
        }
        jdbc.update("""
                INSERT INTO tab_payment (id, establishment_id, tab_id, amount_cents, payment_method, received_at,
                                         received_by)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), owner.establishmentId(), tab, totalCents + serviceCents, method,
                Timestamp.from(closed), owner.userId());
        return tab;
    }

    private void addItems(Owner owner, Place place, UUID tab, Instant created, Item... items) {
        if (items.length == 0) {
            return;
        }
        UUID order = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO customer_order (id, establishment_id, tab_id, table_id, number, status, created_at, placed_by)
                VALUES (?, ?, ?, ?, 1, 'SENT', ?, ?)
                """, order, owner.establishmentId(), tab, place.table(), Timestamp.from(created), owner.userId());
        int position = 0;
        for (Item item : items) {
            jdbc.update("""
                    INSERT INTO order_item (id, establishment_id, order_id, position, product_id, product_name,
                                            unit_price_cents, quantity, status, created_at, canceled_at, canceled_by,
                                            cancel_reason, delivered_at, delivered_by)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, UUID.randomUUID(), owner.establishmentId(), order, position++, item.product(), item.name(),
                    item.priceCents(), item.quantity(), item.canceled() ? "CANCELED" : "DELIVERED",
                    Timestamp.from(created),
                    item.canceled() ? Timestamp.from(created) : null, item.canceled() ? owner.userId() : null,
                    item.canceled() ? "Cliente desistiu" : null,
                    item.canceled() ? null : Timestamp.from(created), item.canceled() ? null : owner.userId());
        }
    }

    private void otherTabs(Owner owner, Place place, Instant opened) {
        // Aberta (ainda não faturou) e juntada (o que tinha está na de destino): nenhuma das duas conta.
        jdbc.update("INSERT INTO tab (id, establishment_id, table_id, status, opened_at) VALUES (?, ?, ?, 'OPEN', ?)",
                UUID.randomUUID(), owner.establishmentId(), place.table(), Timestamp.from(opened));
        UUID into = closedTab(owner, place(owner, "Varanda"), opened, opened.plusSeconds(60), 0, "PIX", 0);
        jdbc.update("""
                INSERT INTO tab (id, establishment_id, table_id, status, opened_at, merged_into_tab_id)
                VALUES (?, ?, ?, 'MERGED', ?, ?)
                """, UUID.randomUUID(), owner.establishmentId(), place.table(), Timestamp.from(opened), into);
    }

    private Item item(UUID product, String name, long price, int quantity) {
        return new Item(product, name, price, quantity, false);
    }

    // ---------- chamadas ----------

    private MvcTestResult get(Owner owner, String uri) {
        return get(owner.token(), uri);
    }

    private MvcTestResult get(String token, String uri) {
        return mvc.get().uri(uri).header(HttpHeaders.AUTHORIZATION, "Bearer " + token).exchange();
    }

    private String overview(LocalDate from, LocalDate to) {
        return "/api/v1/reports/overview?from=%s&to=%s".formatted(from, to);
    }

    private static String json(MvcTestResult result, String path) throws Exception {
        Object value = JsonPath.read(result.getResponse().getContentAsString(), path);
        return value == null ? null : value.toString();
    }

    private static <T> T read(MvcTestResult result, String path) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), path);
    }

    private void onPlan(Owner owner, String code) {
        jdbc.update("""
                UPDATE subscription SET plan_id = (SELECT id FROM plan WHERE code = ?)
                WHERE organization_id = (SELECT organization_id FROM establishment WHERE id = ?)
                """, code, owner.establishmentId());
    }

    private String token(Owner owner, Role role) {
        return tokenIssuer.issue(new TokenSubject(new UserId(owner.userId()), new EstablishmentId(owner.establishmentId()),
                role)).value();
    }

    // ---------- Visão geral ----------

    @Test
    void theOverviewSumsOnlyTheTabsClosedInThePeriodOfTheRestaurant() throws Exception {
        LocalDate first = day.minusDays(6);
        closedTab(sakura, salao, at(first, 12, 10), at(first, 13, 0), 5000, "PIX", 500,
                item(prato, "Prato da casa", 2500, 2));
        closedTab(sakura, salao, at(day, 20, 5), at(day, 21, 30), 7000, "CARD", 0,
                item(chopp, "Chopp 500 ml", 1500, 4), new Item(prato, "Prato da casa", 2500, 1, true));
        // 23h50 em São Paulo ainda é o último dia; 0h10 do dia seguinte, não.
        closedTab(sakura, salao, at(day, 22, 40), at(day, 23, 50), 2500, "PIX", 250,
                item(prato, "Prato da casa", 2500, 1));
        closedTab(sakura, salao, at(day.plusDays(1), 0, 1), at(day.plusDays(1), 0, 10), 9900, "CASH", 0);
        // A semana anterior (a comparação).
        closedTab(sakura, salao, at(first.minusDays(1), 20, 0), at(first.minusDays(1), 21, 0), 3000, "CASH", 0);
        otherTabs(sakura, salao, at(day, 19, 0));

        MvcTestResult result = get(sakura, overview(first, day));

        assertThat(result).hasStatusOk();
        assertThat(json(result, "$.level")).isEqualTo("BASIC");
        assertThat(json(result, "$.current.revenueCents")).isEqualTo("14500");
        // A comanda de destino da junção fechou com zero: conta como comanda, sem somar.
        assertThat(json(result, "$.current.tabs")).isEqualTo("4");
        assertThat(json(result, "$.current.averageTicketCents")).isEqualTo("3625");
        assertThat(json(result, "$.compared.revenueCents")).isEqualTo("3000");
        assertThat(json(result, "$.comparison.from")).isEqualTo(first.minusDays(7).toString());
        assertThat(json(result, "$.comparison.to")).isEqualTo(first.minusDays(1).toString());

        List<Map<String, Object>> byDay = read(result, "$.byDay");
        assertThat(byDay).hasSize(7);
        assertThat(byDay.getFirst()).containsEntry("revenueCents", 5000);
        assertThat(byDay.getLast()).containsEntry("revenueCents", 9500).containsEntry("comparedRevenueCents", 3000);

        // O caixa: o que entrou, com a taxa de serviço.
        assertThat(read(result, "$.payments[?(@.method == 'PIX')].amountCents").toString()).isEqualTo("[8250]");
        assertThat(read(result, "$.payments[?(@.method == 'CARD')].amountCents").toString()).isEqualTo("[7000]");
        assertThat(read(result, "$.payments[?(@.method == 'CASH')]").toString()).isEqualTo("[]");

        // Por faturamento, sem os cancelados: o prato vendeu 3 (2 + 1, e não 4), somando R$ 75,00.
        assertThat(json(result, "$.topItems[0].name")).isEqualTo("Prato da casa");
        assertThat(json(result, "$.topItems[0].quantity")).isEqualTo("3");
        assertThat(json(result, "$.topItems[0].revenueCents")).isEqualTo("7500");
        assertThat(json(result, "$.topItems[1].name")).isEqualTo("Chopp 500 ml");
        assertThat(json(result, "$.topItems[1].revenueCents")).isEqualTo("6000");
    }

    @Test
    void theHeatmapIsTheAveragePerWeekdayByTheHourTheTableArrived() throws Exception {
        LocalDate first = day.minusDays(13);
        // Duas comandas no mesmo dia da semana (uma por semana), abertas às 21h: média = metade da soma.
        closedTab(sakura, salao, at(first, 21, 5), at(first, 22, 0), 4000, "PIX", 0);
        closedTab(sakura, salao, at(first.plusDays(7), 21, 40), at(first.plusDays(7), 23, 0), 6000, "PIX", 0);

        MvcTestResult result = get(sakura, overview(first, day));

        List<Map<String, Object>> heat = read(result, "$.heatmap");
        assertThat(heat).hasSize(1);
        assertThat(heat.getFirst()).containsEntry("weekday", first.getDayOfWeek().getValue())
                .containsEntry("hour", 21).containsEntry("averageCents", 5000);
    }

    @Test
    void theSectorFilterCountsOnlyItsTables() throws Exception {
        Place deck = place(sakura, "Deck");
        closedTab(sakura, salao, at(day, 20, 0), at(day, 21, 0), 5000, "PIX", 0);
        closedTab(sakura, deck, at(day, 20, 0), at(day, 21, 0), 9000, "CARD", 0);

        MvcTestResult all = get(sakura, overview(day, day));
        MvcTestResult onlyDeck = get(sakura, overview(day, day) + "&sectorId=" + deck.sector());

        assertThat(json(all, "$.current.revenueCents")).isEqualTo("14000");
        assertThat(json(onlyDeck, "$.current.revenueCents")).isEqualTo("9000");
        assertThat(read(onlyDeck, "$.payments[*].method").toString()).isEqualTo("[\"CARD\"]");
    }

    @Test
    void anotherRestaurantsSalesAndSectorsNeverShowUp() throws Exception {
        Owner other = newRestaurant();
        Place theirs = place(other, "Salão");
        UUID theirDish = product(other, "Pizza grande", 7490);
        closedTab(other, theirs, at(day, 20, 0), at(day, 21, 0), 7490, "PIX", 0, item(theirDish, "Pizza grande", 7490, 1));
        closedTab(sakura, salao, at(day, 20, 0), at(day, 21, 0), 2500, "CASH", 0, item(prato, "Prato da casa", 2500, 1));

        MvcTestResult mine = get(sakura, overview(day, day));

        assertThat(json(mine, "$.current.revenueCents")).isEqualTo("2500");
        assertThat(read(mine, "$.topItems[*].name").toString()).isEqualTo("[\"Prato da casa\"]");
        assertThat(read(mine, "$.payments[*].method").toString()).isEqualTo("[\"CASH\"]");
        // O setor de outro restaurante: 404, sem revelar que existe.
        assertThat(get(sakura, overview(day, day) + "&sectorId=" + theirs.sector())).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(get(sakura, "/api/v1/reports/sales?from=%s&to=%s&sectorId=%s".formatted(day, day, theirs.sector())))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void whenThePeriodEndsTodayTheComparisonStopsAtTheSameTime() throws Exception {
        ZonedDateTime now = ZonedDateTime.now(SAO_PAULO);
        // Precisa de folga para "uma hora antes" e "uma hora depois" caírem no mesmo dia.
        assumeTrue(now.getHour() >= 2 && now.getHour() <= 21, "perto da meia-noite");
        ZonedDateTime lastWeek = now.minusDays(7);
        closedTab(sakura, salao, lastWeek.minusHours(2).toInstant(), lastWeek.minusHours(1).toInstant(), 3000, "PIX", 0);
        closedTab(sakura, salao, lastWeek.toInstant(), lastWeek.plusHours(1).toInstant(), 8000, "PIX", 0);

        LocalDate today = now.toLocalDate();
        MvcTestResult result = get(sakura, overview(today, today));

        assertThat(json(result, "$.comparison.from")).isEqualTo(today.minusDays(7).toString());
        // Só a que fechou antes desta hora, na semana passada.
        assertThat(json(result, "$.compared.revenueCents")).isEqualTo("3000");
    }

    // ---------- Vendas ----------

    @Test
    void salesGroupByWeekAndFindTheBestDayAndThePeakHour() throws Exception {
        LocalDate first = day.minusDays(13);
        closedTab(sakura, salao, at(first, 12, 0), at(first, 13, 0), 2000, "PIX", 0);
        closedTab(sakura, salao, at(day, 21, 10), at(day, 22, 0), 9000, "PIX", 0);
        closedTab(sakura, salao, at(day, 21, 30), at(day, 22, 30), 5000, "CARD", 0);

        MvcTestResult result = get(sakura, "/api/v1/reports/sales?from=%s&to=%s&groupBy=WEEK".formatted(first, day));

        assertThat(result).hasStatusOk();
        assertThat(json(result, "$.grouping")).isEqualTo("WEEK");
        List<Map<String, Object>> series = read(result, "$.series");
        assertThat(series.stream().mapToLong(p -> ((Number) p.get("revenueCents")).longValue()).sum()).isEqualTo(16000);
        assertThat(series.getFirst()).containsEntry("start", first.toString());
        assertThat(series.getLast()).containsEntry("end", day.toString());
        assertThat(json(result, "$.bestDay.day")).isEqualTo(day.toString());
        assertThat(json(result, "$.bestDay.revenueCents")).isEqualTo("14000");
        assertThat(json(result, "$.peakHour")).isEqualTo("21");
        assertThat(json(result, "$.current.tabs")).isEqualTo("3");
        assertThat(json(result, "$.tabsPerDay")).isEqualTo("0.2");
        List<Map<String, Object>> hours = read(result, "$.byHour");
        assertThat(hours).extracting(h -> h.get("hour")).containsExactly(12, 21);
        assertThat(hours.getLast()).containsEntry("revenuePerDayCents", 1000);
    }

    @Test
    void aPeriodWithoutSalesComesWithZerosAndNoBestDay() throws Exception {
        MvcTestResult sales = get(sakura, "/api/v1/reports/sales?from=%s&to=%s".formatted(day.minusDays(2), day));
        MvcTestResult overview = get(sakura, overview(day.minusDays(2), day));

        assertThat(json(sales, "$.current.revenueCents")).isEqualTo("0");
        assertThat(json(sales, "$.current.averageTicketCents")).isEqualTo("0");
        assertThat(json(sales, "$.bestDay")).isNull();
        assertThat(json(sales, "$.peakHour")).isNull();
        assertThat(read(sales, "$.series").toString()).contains("\"revenueCents\":0");
        assertThat(read(overview, "$.byDay[*].revenueCents").toString()).isEqualTo("[0,0,0]");
        assertThat(read(overview, "$.topItems").toString()).isEqualTo("[]");
    }

    // ---------- plano, permissão e período ----------

    @Test
    void theEssencialHasNoReports() throws Exception {
        onPlan(sakura, "essential");

        MvcTestResult result = get(sakura, overview(day, day));

        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(json(result, "$.detail")).contains("Pro");
        assertThat(get(sakura, "/api/v1/reports/sales?from=%s&to=%s".formatted(day, day))).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void onlyTheOwnerAndTheManagerSeeTheSales() {
        assertThat(get(token(sakura, Role.MANAGER), overview(day, day))).hasStatusOk();
        for (Role role : new Role[]{Role.STAFF, Role.KITCHEN}) {
            assertThat(get(token(sakura, role), overview(day, day))).hasStatus(HttpStatus.FORBIDDEN);
            assertThat(get(token(sakura, role), "/api/v1/reports/sales?from=%s&to=%s".formatted(day, day)))
                    .hasStatus(HttpStatus.FORBIDDEN);
        }
    }

    @Test
    void thePeriodMustMakeSense() throws Exception {
        LocalDate today = LocalDate.now(SAO_PAULO);
        // Fim antes do início, e mais de 12 meses de uma vez: 400.
        assertThat(get(sakura, overview(day, day.minusDays(1)))).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(get(sakura, overview(day.minusDays(366), day))).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(get(sakura, "/api/v1/reports/overview?from=" + day)).hasStatus(HttpStatus.BAD_REQUEST);
        // O futuro e, no Pro, antes dos 12 meses de histórico: 422.
        assertThat(get(sakura, overview(today, today.plusDays(1)))).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        MvcTestResult tooOld = get(sakura, overview(today.minusDays(400), today.minusDays(390)));
        assertThat(tooOld).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(json(tooOld, "$.detail")).contains("12 meses");
        // O Business não tem limite de histórico.
        onPlan(sakura, "business");
        MvcTestResult old = get(sakura, overview(today.minusDays(400), today.minusDays(390)));
        assertThat(old).hasStatusOk();
        assertThat(json(old, "$.level")).isEqualTo("FULL");
    }
}
