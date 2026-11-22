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
import static br.com.cardapio.adapter.in.web.report.ReportTestData.json;
import static br.com.cardapio.adapter.in.web.report.ReportTestData.prep;
import static br.com.cardapio.adapter.in.web.report.ReportTestData.read;
import static org.assertj.core.api.Assertions.assertThat;

/** A cozinha completa (R8): passe, tempo até a mesa, por hora, pico, por categoria e os que demoraram. */
@IntegrationTest
class KitchenFullReportControllerTest {

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

    /**
     * Às 20h do último dia: guioza de 10 min (2 min no passe), guioza de 20 min (8 min no passe) e chá de 2
     * min (1 min no passe); às 20h05, yakissoba de 15 min (10 min no passe). No primeiro dia, um temaki que
     * esperou 10 min a confirmação do garçom e ficou pronto em 5 min.
     */
    @BeforeEach
    void sakuraWithAKitchenNight() throws Exception {
        data = new ReportTestData(mvc, jdbc, tokenIssuer);
        sakura = data.newRestaurant();
        salao = data.place(sakura, "Salão");
        Instant old = Instant.now().minus(Duration.ofDays(60));
        UUID pratos = data.category(sakura, "Pratos");
        UUID bebidas = data.category(sakura, "Bebidas");
        UUID guioza = data.product(sakura, pratos, "Guioza", 2900, old);
        UUID yakissoba = data.product(sakura, pratos, "Yakissoba", 4900, old);
        UUID temaki = data.product(sakura, pratos, "Temaki", 3200, old);
        UUID cha = data.product(sakura, bebidas, "Chá", 900, old);

        data.kitchenOrder(sakura, salao, at(day, 20, 0), null, prep(guioza, "Guioza", 10), prep(guioza, "Guioza", 20),
                prep(cha, "Chá", 2));
        data.kitchenOrder(sakura, salao, at(day, 20, 5), null, prep(yakissoba, "Yakissoba", 15));
        data.kitchenOrder(sakura, salao, at(first, 12, 0), at(first, 12, 10), prep(temaki, "Temaki", 5));
        // A entrega: o primeiro guioza 2 min depois de pronto, o segundo 8, o yakissoba 10 e o chá 1.
        deliver(guioza, 10, 2);
        deliver(guioza, 20, 8);
        deliver(yakissoba, 15, 10);
        deliver(cha, 2, 1);
    }

    /** Entrega o item do produto que ficou pronto em {@code prep} min, {@code passe} min depois de pronto. */
    private void deliver(UUID product, int prep, int passe) {
        jdbc.update("""
                UPDATE order_item SET status = 'DELIVERED', delivered_at = ready_at + make_interval(mins => ?)
                WHERE product_id = ? AND ready_at - preparing_at = make_interval(mins => ?)
                """, passe, product, prep);
    }

    private String kitchen() {
        return "/api/v1/reports/kitchen?from=%s&to=%s".formatted(first, day);
    }

    @Test
    void theProGetsNoneOfTheBusinessBlocks() throws Exception {
        assertThat(json(data.get(sakura, kitchen()), "$.full")).isNull();
    }

    @Test
    void thePasseTheTimeToTheTableAndTheLateConfirmations() throws Exception {
        data.onPlan(sakura, "business");

        MvcTestResult result = data.get(sakura, kitchen());

        // (2 + 8 + 10 + 1) / 4 min; (12 + 28 + 25 + 3) / 4 min.
        assertThat(json(result, "$.full.current.passeSeconds")).isEqualTo("315");
        assertThat(json(result, "$.full.current.toTableSeconds")).isEqualTo("1020");
        assertThat(json(result, "$.full.current.lateConfirmations")).isEqualTo("1");
        assertThat(json(result, "$.full.compared.passeSeconds")).isNull();
    }

    @Test
    void byHourPeaksCategoriesAndTheDelayed() throws Exception {
        data.onPlan(sakura, "business");

        MvcTestResult result = data.get(sakura, kitchen());

        assertThat(json(result, "$.full.byHour[0].hour")).isEqualTo("20");
        assertThat(json(result, "$.full.byHour[0].prepSeconds")).isEqualTo("705");
        assertThat(json(result, "$.full.byHour[0].items")).isEqualTo("4");
        // Às 20h, até 3 itens no fogo ao mesmo tempo; ao meio-dia (às 12h10), o temaki sozinho.
        assertThat(read(result, "$.full.peaks[*].hour").toString()).isEqualTo("[12,20]");
        assertThat(json(result, "$.full.peaks[1].average")).isEqualTo("3.0");
        List<String> categories = read(result, "$.full.byCategory[*].name");
        assertThat(categories).containsExactly("Pratos", "Bebidas");
        assertThat(json(result, "$.full.byCategory[0].averageSeconds")).isEqualTo("750");
        // Os dois que esperaram mais de 5 min no passe ficaram prontos às 20h20.
        assertThat(json(result, "$.full.delayed[0].day")).isEqualTo(day.toString());
        assertThat(json(result, "$.full.delayed[0].hour")).isEqualTo("20");
        assertThat(json(result, "$.full.delayed[0].items")).isEqualTo("2");
        assertThat(json(result, "$.full.delayed[0].averageWaitSeconds")).isEqualTo("540");
    }

    @Test
    void theCategoryFilterAndAnotherRestaurant() throws Exception {
        data.onPlan(sakura, "business");
        UUID bebidas = jdbc.queryForObject("SELECT id FROM menu_category WHERE establishment_id = ? AND name = 'Bebidas'",
                UUID.class, sakura.establishmentId());
        Owner other = data.newRestaurant();
        data.onPlan(other, "business");
        Place theirs = data.place(other, "Salão");
        UUID pizza = data.product(other, "Pizza", 5000);
        data.kitchenOrder(other, theirs, at(day, 20, 0), at(day, 20, 30), prep(pizza, "Pizza", 40));

        MvcTestResult drinks = data.get(sakura, kitchen() + "&categoryId=" + bebidas);
        MvcTestResult mine = data.get(sakura, kitchen());
        MvcTestResult theirsReport = data.get(other, kitchen());

        assertThat(read(drinks, "$.full.byCategory[*].name").toString()).isEqualTo("[\"Bebidas\"]");
        assertThat(json(drinks, "$.full.current.passeSeconds")).isEqualTo("60");
        assertThat(read(mine, "$.full.byCategory[*].name").toString()).doesNotContain("Pizzas");
        assertThat(json(mine, "$.full.current.lateConfirmations")).isEqualTo("1");
        assertThat(json(theirsReport, "$.full.current.lateConfirmations")).isEqualTo("1");
        assertThat(json(theirsReport, "$.full.byCategory[0].averageSeconds")).isEqualTo("2400");
    }
}
