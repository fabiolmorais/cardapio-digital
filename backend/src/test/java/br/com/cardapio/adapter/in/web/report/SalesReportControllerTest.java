package br.com.cardapio.adapter.in.web.report;

import br.com.cardapio.application.port.out.security.AccessTokenIssuer;
import br.com.cardapio.domain.account.model.Role;
import br.com.cardapio.adapter.in.web.report.ReportTestData.Owner;
import br.com.cardapio.adapter.in.web.report.ReportTestData.Place;
import br.com.cardapio.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static br.com.cardapio.adapter.in.web.report.ReportTestData.SAO_PAULO;
import static br.com.cardapio.adapter.in.web.report.ReportTestData.at;
import static br.com.cardapio.adapter.in.web.report.ReportTestData.canceled;
import static br.com.cardapio.adapter.in.web.report.ReportTestData.item;
import static br.com.cardapio.adapter.in.web.report.ReportTestData.json;
import static br.com.cardapio.adapter.in.web.report.ReportTestData.read;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** A Visão geral e Vendas (R1): as somas das comandas fechadas, sempre de um restaurante só. */
@IntegrationTest
class SalesReportControllerTest {


    @Autowired
    MockMvcTester mvc;

    @Autowired
    AccessTokenIssuer tokenIssuer;

    @Autowired
    JdbcTemplate jdbc;

    /** O último dia do período dos testes: anteontem, um dia inteiro no passado. */
    private final LocalDate day = LocalDate.now(SAO_PAULO).minusDays(2);

    private ReportTestData data;
    private Owner sakura;
    private Place salao;
    private UUID prato;
    private UUID chopp;

    @BeforeEach
    void sakuraWithASalon() throws Exception {
        data = new ReportTestData(mvc, jdbc, tokenIssuer);
        sakura = data.newRestaurant();
        salao = data.place(sakura, "Salão");
        prato = data.product(sakura, "Prato da casa", 2500);
        chopp = data.product(sakura, "Chopp 500 ml", 1500);
    }

    private MvcTestResult get(Owner owner, String uri) {
        return data.get(owner, uri);
    }

    private MvcTestResult get(String token, String uri) {
        return data.get(token, uri);
    }

    private String overview(LocalDate from, LocalDate to) {
        return "/api/v1/reports/overview?from=%s&to=%s".formatted(from, to);
    }

    /** Uma comanda aberta e uma juntada (com a de destino fechada com zero): nenhuma das duas soma. */
    private void otherTabs(Owner owner, Place place, Instant opened) {
        data.openTab(owner, place, opened);
        UUID into = data.closedTab(owner, data.place(owner, "Varanda"), opened, opened.plusSeconds(60), 0, "PIX", 0);
        data.mergedTab(owner, place, opened, into);
    }

    // ---------- Visão geral ----------

    @Test
    void theOverviewSumsOnlyTheTabsClosedInThePeriodOfTheRestaurant() throws Exception {
        LocalDate first = day.minusDays(6);
        data.closedTab(sakura, salao, at(first, 12, 10), at(first, 13, 0), 5000, "PIX", 500,
                item(prato, "Prato da casa", 2500, 2));
        data.closedTab(sakura, salao, at(day, 20, 5), at(day, 21, 30), 7000, "CARD", 0,
                item(chopp, "Chopp 500 ml", 1500, 4), canceled(prato, "Prato da casa", 2500, 1));
        // 23h50 em São Paulo ainda é o último dia; 0h10 do dia seguinte, não.
        data.closedTab(sakura, salao, at(day, 22, 40), at(day, 23, 50), 2500, "PIX", 250,
                item(prato, "Prato da casa", 2500, 1));
        data.closedTab(sakura, salao, at(day.plusDays(1), 0, 1), at(day.plusDays(1), 0, 10), 9900, "CASH", 0);
        // A semana anterior (a comparação).
        data.closedTab(sakura, salao, at(first.minusDays(1), 20, 0), at(first.minusDays(1), 21, 0), 3000, "CASH", 0);
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
        data.closedTab(sakura, salao, at(first, 21, 5), at(first, 22, 0), 4000, "PIX", 0);
        data.closedTab(sakura, salao, at(first.plusDays(7), 21, 40), at(first.plusDays(7), 23, 0), 6000, "PIX", 0);

        MvcTestResult result = get(sakura, overview(first, day));

        List<Map<String, Object>> heat = read(result, "$.heatmap");
        assertThat(heat).hasSize(1);
        assertThat(heat.getFirst()).containsEntry("weekday", first.getDayOfWeek().getValue())
                .containsEntry("hour", 21).containsEntry("averageCents", 5000);
    }

    @Test
    void theSectorFilterCountsOnlyItsTables() throws Exception {
        Place deck = data.place(sakura, "Deck");
        data.closedTab(sakura, salao, at(day, 20, 0), at(day, 21, 0), 5000, "PIX", 0);
        data.closedTab(sakura, deck, at(day, 20, 0), at(day, 21, 0), 9000, "CARD", 0);

        MvcTestResult all = get(sakura, overview(day, day));
        MvcTestResult onlyDeck = get(sakura, overview(day, day) + "&sectorId=" + deck.sector());

        assertThat(json(all, "$.current.revenueCents")).isEqualTo("14000");
        assertThat(json(onlyDeck, "$.current.revenueCents")).isEqualTo("9000");
        assertThat(read(onlyDeck, "$.payments[*].method").toString()).isEqualTo("[\"CARD\"]");
    }

    @Test
    void anotherRestaurantsSalesAndSectorsNeverShowUp() throws Exception {
        Owner other = data.newRestaurant();
        Place theirs = data.place(other, "Salão");
        UUID theirDish = data.product(other, "Pizza grande", 7490);
        data.closedTab(other, theirs, at(day, 20, 0), at(day, 21, 0), 7490, "PIX", 0,
                item(theirDish, "Pizza grande", 7490, 1));
        data.closedTab(sakura, salao, at(day, 20, 0), at(day, 21, 0), 2500, "CASH", 0,
                item(prato, "Prato da casa", 2500, 1));

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
        data.closedTab(sakura, salao, lastWeek.minusHours(2).toInstant(), lastWeek.minusHours(1).toInstant(), 3000,
                "PIX", 0);
        data.closedTab(sakura, salao, lastWeek.toInstant(), lastWeek.plusHours(1).toInstant(), 8000, "PIX", 0);

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
        data.closedTab(sakura, salao, at(first, 12, 0), at(first, 13, 0), 2000, "PIX", 0);
        data.closedTab(sakura, salao, at(day, 21, 10), at(day, 22, 0), 9000, "PIX", 0);
        data.closedTab(sakura, salao, at(day, 21, 30), at(day, 22, 30), 5000, "CARD", 0);

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
        data.onPlan(sakura, "essential");

        MvcTestResult result = get(sakura, overview(day, day));

        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(json(result, "$.detail")).contains("Pro");
        assertThat(get(sakura, "/api/v1/reports/sales?from=%s&to=%s".formatted(day, day))).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void onlyTheOwnerAndTheManagerSeeTheSales() {
        assertThat(get(data.token(sakura, Role.MANAGER), overview(day, day))).hasStatusOk();
        for (Role role : new Role[]{Role.STAFF, Role.KITCHEN}) {
            assertThat(get(data.token(sakura, role), overview(day, day))).hasStatus(HttpStatus.FORBIDDEN);
            assertThat(get(data.token(sakura, role), "/api/v1/reports/sales?from=%s&to=%s".formatted(day, day)))
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
        data.onPlan(sakura, "business");
        MvcTestResult old = get(sakura, overview(today.minusDays(400), today.minusDays(390)));
        assertThat(old).hasStatusOk();
        assertThat(json(old, "$.level")).isEqualTo("FULL");
    }
}
