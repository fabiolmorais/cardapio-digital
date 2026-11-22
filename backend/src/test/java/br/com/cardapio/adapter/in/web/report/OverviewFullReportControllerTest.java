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

import java.time.Duration;
import java.time.Instant;
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

/** A Visão geral completa (R6a): os números de operação, hoje até agora, os garçons e os pontos de atenção. */
@IntegrationTest
class OverviewFullReportControllerTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    AccessTokenIssuer tokenIssuer;

    @Autowired
    JdbcTemplate jdbc;

    private final LocalDate today = LocalDate.now(SAO_PAULO);
    private final LocalDate day = today.minusDays(2);
    private final LocalDate first = day.minusDays(6);

    private ReportTestData data;
    private Owner sakura;
    private Place salao;
    private UUID prato;
    private UUID chopp;

    /**
     * Na semana: duas comandas de 90 e 60 min (uma com um prato cancelado de R$ 25,00), uma de 30 min com
     * 3 chopes cancelados por "Cliente desistiu", um pedido pelo celular, três chamados (Carlos 60 s e
     * 120 s, Ana 30 s) e uma comanda esquecida aberta desde o último dia. Os itens entregues chegaram à
     * mesa em 20 min.
     */
    @BeforeEach
    void sakuraWithAWeek() throws Exception {
        data = new ReportTestData(mvc, jdbc, tokenIssuer);
        sakura = data.newRestaurant();
        salao = data.place(sakura, "Salão");
        prato = data.product(sakura, "Prato da casa", 2500);
        chopp = data.product(sakura, "Chopp 500 ml", 1500);

        UUID dinner = data.closedTab(sakura, salao, at(day, 20, 0), at(day, 21, 30), 5000, "PIX", 0,
                item(prato, "Prato da casa", 2500, 2));
        data.closedTab(sakura, salao, at(day, 12, 0), at(day, 13, 0), 1500, "CARD", 0,
                item(chopp, "Chopp 500 ml", 1500, 1), canceled(prato, "Prato da casa", 2500, 1));
        data.closedTab(sakura, salao, at(first, 18, 0), at(first, 18, 30), 0, "PIX", 0,
                canceled(chopp, "Chopp 500 ml", 1500, 3));
        jdbc.update("UPDATE order_item SET delivered_at = created_at + interval '20 minutes' "
                + "WHERE establishment_id = ? AND status = 'DELIVERED'", sakura.establishmentId());

        UUID phone = data.session(sakura, salao, at(day, 20, 0));
        data.phoneOrder(sakura, salao, dinner, phone, at(day, 20, 10));
        UUID carlos = data.waiter(sakura, "Carlos");
        UUID ana = data.waiter(sakura, "Ana");
        data.call(sakura, salao, phone, at(day, 20, 15), at(day, 20, 16), carlos);
        data.call(sakura, salao, phone, at(day, 20, 30), at(day, 20, 32), carlos);
        data.call(sakura, salao, phone, at(day, 21, 0), at(day, 21, 0).plusSeconds(30), ana);
        data.call(sakura, salao, phone, at(day, 21, 10), null, null);

        data.openTab(sakura, data.place(sakura, "Varanda"), at(day, 23, 0));
    }

    private String overview() {
        return "/api/v1/reports/overview?from=%s&to=%s".formatted(first, day);
    }

    @Test
    void theProGetsNoneOfTheBusinessBlocks() throws Exception {
        MvcTestResult result = data.get(sakura, overview());

        assertThat(json(result, "$.level")).isEqualTo("BASIC");
        assertThat(json(result, "$.full")).isNull();
    }

    @Test
    void theBusinessSeesTheOperationNumbers() throws Exception {
        data.onPlan(sakura, "business");

        MvcTestResult result = data.get(sakura, overview());

        assertThat(json(result, "$.level")).isEqualTo("FULL");
        assertThat(json(result, "$.full.current.toTableSeconds")).isEqualTo("1200");
        assertThat(json(result, "$.full.current.phoneOrders")).isEqualTo("1");
        assertThat(json(result, "$.full.current.staffOrders")).isEqualTo("3");
        assertThat(json(result, "$.full.current.phoneSharePercent")).isEqualTo("25.0");
        // (60 + 120 + 30) / 3; o chamado sem resposta fica de fora.
        assertThat(json(result, "$.full.current.callResponseSeconds")).isEqualTo("70");
        // (90 + 60 + 30) / 3 min.
        assertThat(json(result, "$.full.current.staySeconds")).isEqualTo("3600");
        assertThat(json(result, "$.full.current.canceledCents")).isEqualTo("7000");
        assertThat(json(result, "$.full.compared.staffOrders")).isEqualTo("0");
        assertThat(json(result, "$.full.compared.staySeconds")).isNull();
    }

    @Test
    void theWaitersAndTheAttentionPoints() throws Exception {
        data.onPlan(sakura, "business");

        MvcTestResult result = data.get(sakura, overview());

        List<String> waiters = read(result, "$.full.waiters[*].name");
        assertThat(waiters).containsExactly("Carlos", "Ana");
        assertThat(json(result, "$.full.waiters[0].calls")).isEqualTo("2");
        assertThat(json(result, "$.full.waiters[0].averageResponseSeconds")).isEqualTo("90");
        List<String> kinds = read(result, "$.full.attention[*].kind");
        assertThat(kinds).containsExactly("FORGOTTEN_TABS", "CANCELED_PRODUCT");
        assertThat(read(result, "$.full.attention[0].tables").toString()).isEqualTo("[\"Va1\"]");
        assertThat(json(result, "$.full.attention[1].name")).isEqualTo("Chopp 500 ml");
        assertThat(json(result, "$.full.attention[1].times")).isEqualTo("3");
        assertThat(json(result, "$.full.attention[1].reason")).isEqualTo("Cliente desistiu");
        assertThat(json(result, "$.full.attention[1].reasonTimes")).isEqualTo("3");
    }

    @Test
    void todayIsComparedWithTheSameWeekdayOfTheWeeksBefore() throws Exception {
        data.onPlan(sakura, "business");
        Instant now = Instant.now();
        Instant startOfToday = today.atStartOfDay(SAO_PAULO).toInstant();
        Duration elapsed = Duration.between(startOfToday, now);
        // Na 3ª e na 4ª semana antes, o mesmo dia da semana: R$ 100 e R$ 60, os dois antes desta hora.
        for (var week : List.of(new long[]{3, 10000}, new long[]{4, 6000})) {
            Instant closed = today.minusWeeks(week[0]).atStartOfDay(SAO_PAULO).toInstant().plus(elapsed.dividedBy(2));
            data.closedTab(sakura, salao, closed.minusSeconds(60), closed, week[1], "PIX", 0);
        }
        Instant closedToday = now.minusSeconds(10).isBefore(startOfToday) ? startOfToday : now.minusSeconds(10);
        data.closedTab(sakura, salao, closedToday.minusSeconds(60), closedToday, 3000, "PIX", 0);

        MvcTestResult result = data.get(sakura, overview());

        assertThat(json(result, "$.full.today.soFarCents")).isEqualTo("3000");
        assertThat(json(result, "$.full.today.expectedCents")).isEqualTo("8000");
        assertThat(json(result, "$.full.today.usualCents")).isEqualTo("8000");
        assertThat(json(result, "$.full.today.weeks")).isEqualTo("2");
    }

    @Test
    void anotherRestaurantsNumbersNeverShowUp() throws Exception {
        Owner other = data.newRestaurant();
        data.onPlan(other, "business");
        Place theirs = data.place(other, "Salão");
        UUID session = data.session(other, theirs, at(day, 20, 0));
        data.call(other, theirs, session, at(day, 20, 0), at(day, 20, 5), data.waiter(other, "Bruno"));
        data.openTab(other, theirs, at(day, 10, 0));
        data.onPlan(sakura, "business");

        MvcTestResult mine = data.get(sakura, overview());
        MvcTestResult theirsReport = data.get(other, overview());

        assertThat(read(mine, "$.full.waiters[*].name").toString()).doesNotContain("Bruno");
        assertThat(json(mine, "$.full.current.callResponseSeconds")).isEqualTo("70");
        assertThat(read(theirsReport, "$.full.waiters[*].name").toString()).isEqualTo("[\"Bruno\"]");
        assertThat(json(theirsReport, "$.full.current.staffOrders")).isEqualTo("0");
        assertThat(read(theirsReport, "$.full.attention[0].tables").toString()).isEqualTo("[\"Sa1\"]");
    }
}
