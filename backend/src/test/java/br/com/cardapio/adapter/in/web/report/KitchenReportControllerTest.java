package br.com.cardapio.adapter.in.web.report;

import br.com.cardapio.adapter.in.web.report.ReportTestData.Owner;
import br.com.cardapio.adapter.in.web.report.ReportTestData.Place;
import br.com.cardapio.adapter.in.web.report.ReportTestData.Prep;
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

/** Cozinha e tempos (R3): o preparo médio, com a comparação, e os itens que mais demoram. */
@IntegrationTest
class KitchenReportControllerTest {

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
    private UUID bebidas;

    /**
     * Na semana: o guioza fica pronto 5 vezes (8, 9, 10, 11 e 31 min, às 20h), o yakissoba 5 (15, 17, 19,
     * 21 e 28 min, às 21h), o chá 4 (40 min) e o temaki 1 (12 min contados da confirmação do garçom).
     * Não contam: um guioza cancelado e um ainda no fogo. Na semana anterior: um guioza de 10 min.
     */
    @BeforeEach
    void sakuraWithAWeekInTheKitchen() throws Exception {
        data = new ReportTestData(mvc, jdbc, tokenIssuer);
        sakura = data.newRestaurant();
        salao = data.place(sakura, "Salão");
        Instant twoMonthsAgo = Instant.now().minus(Duration.ofDays(60));
        UUID pratos = data.category(sakura, "Pratos");
        bebidas = data.category(sakura, "Bebidas");
        UUID guioza = data.product(sakura, pratos, "Guioza", 2900, twoMonthsAgo);
        UUID yakissoba = data.product(sakura, pratos, "Yakissoba", 4900, twoMonthsAgo);
        UUID temaki = data.product(sakura, pratos, "Temaki", 3200, twoMonthsAgo);
        UUID cha = data.product(sakura, bebidas, "Chá gelado", 900, twoMonthsAgo);

        data.kitchenOrder(sakura, salao, at(day, 20, 0), null, prep(guioza, "Guioza", 8), prep(guioza, "Guioza", 9),
                prep(guioza, "Guioza", 10), prep(guioza, "Guioza", 11), prep(guioza, "Guioza", 31),
                new Prep(guioza, "Guioza", Duration.ofMinutes(2), true),
                new Prep(guioza, "Guioza", null, false));
        data.kitchenOrder(sakura, salao, at(day, 21, 0), null, prep(yakissoba, "Yakissoba", 15),
                prep(yakissoba, "Yakissoba", 17), prep(yakissoba, "Yakissoba", 19), prep(yakissoba, "Yakissoba", 21),
                prep(yakissoba, "Yakissoba", 28));
        data.kitchenOrder(sakura, salao, at(first, 13, 0), null, prep(cha, "Chá gelado", 40),
                prep(cha, "Chá gelado", 40), prep(cha, "Chá gelado", 40), prep(cha, "Chá gelado", 40));
        // O cliente pediu às 12h, o garçom confirmou às 12h10 e ficou pronto às 12h22: 12 min de preparo.
        data.kitchenOrder(sakura, salao, at(first, 12, 0), at(first, 12, 10), prep(temaki, "Temaki", 12));
        data.kitchenOrder(sakura, salao, at(first.minusDays(3), 20, 0), null, prep(guioza, "Guioza", 10));
    }

    private String kitchen(String extra) {
        return "/api/v1/reports/kitchen?from=%s&to=%s%s".formatted(first, day, extra);
    }

    @Test
    void theAveragePrepWithTheComparison() throws Exception {
        MvcTestResult result = data.get(sakura, kitchen(""));

        assertThat(result).hasStatusOk();
        assertThat(json(result, "$.level")).isEqualTo("BASIC");
        assertThat(json(result, "$.comparison.to")).isEqualTo(first.minusDays(1).toString());
        assertThat(json(result, "$.preparations")).isEqualTo("15");
        // (69 + 100 + 160 + 12) min / 15 = 22 min 44 s
        assertThat(json(result, "$.averagePrepSeconds")).isEqualTo("1364");
        assertThat(json(result, "$.comparedAveragePrepSeconds")).isEqualTo("600");
    }

    @Test
    void theSlowestNeedFivePreparationsAndGoByNineInTen() throws Exception {
        MvcTestResult result = data.get(sakura, kitchen(""));

        List<String> names = read(result, "$.slowest[*].name");
        assertThat(names).containsExactly("Guioza", "Yakissoba");
        assertThat(json(result, "$.slowest[0].category")).isEqualTo("Pratos");
        assertThat(json(result, "$.slowest[0].preparations")).isEqualTo("5");
        assertThat(json(result, "$.slowest[0].averageSeconds")).isEqualTo("828");
        assertThat(json(result, "$.slowest[0].ninetyPercentSeconds")).isEqualTo("1860");
        assertThat(json(result, "$.slowest[0].worstWeekday")).isEqualTo(String.valueOf(day.getDayOfWeek().getValue()));
        assertThat(json(result, "$.slowest[0].worstHour")).isEqualTo("20");
        assertThat(json(result, "$.slowest[1].worstHour")).isEqualTo("21");
    }

    @Test
    void theCategoryFilterNarrowsEverything() throws Exception {
        MvcTestResult result = data.get(sakura, kitchen("&categoryId=" + bebidas));

        assertThat(json(result, "$.preparations")).isEqualTo("4");
        assertThat(json(result, "$.averagePrepSeconds")).isEqualTo("2400");
        assertThat(json(result, "$.comparedAveragePrepSeconds")).isNull();
        // O chá ficou pronto só 4 vezes: não entra nos que mais demoram.
        assertThat(read(result, "$.slowest").toString()).isEqualTo("[]");
    }

    @Test
    void aPeriodWithoutPreparationsHasNoAverage() throws Exception {
        LocalDate today = LocalDate.now(SAO_PAULO);
        MvcTestResult result = data.get(sakura, "/api/v1/reports/kitchen?from=%s&to=%s".formatted(today, today));

        assertThat(result).hasStatusOk();
        assertThat(json(result, "$.preparations")).isEqualTo("0");
        assertThat(json(result, "$.averagePrepSeconds")).isNull();
    }

    @Test
    void anotherRestaurantsKitchenNeverShowsUp() throws Exception {
        Owner other = data.newRestaurant();
        Place theirs = data.place(other, "Salão");
        UUID theirCategory = data.category(other, "Pizzas");
        UUID pizza = data.product(other, theirCategory, "Pizza grande", 7490, Instant.now().minus(Duration.ofDays(60)));
        data.kitchenOrder(other, theirs, at(day, 20, 0), null, prep(pizza, "Pizza grande", 50),
                prep(pizza, "Pizza grande", 50), prep(pizza, "Pizza grande", 50), prep(pizza, "Pizza grande", 50),
                prep(pizza, "Pizza grande", 50));

        MvcTestResult mine = data.get(sakura, kitchen(""));

        assertThat(json(mine, "$.preparations")).isEqualTo("15");
        assertThat(read(mine, "$.slowest[*].name").toString()).doesNotContain("Pizza");
        // A categoria ou o setor de outro restaurante: 404, sem revelar que existe.
        assertThat(data.get(sakura, kitchen("&categoryId=" + theirCategory))).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(data.get(sakura, kitchen("&sectorId=" + theirs.sector()))).hasStatus(HttpStatus.NOT_FOUND);
        // E o outro só vê a pizza.
        MvcTestResult theirReport = data.get(other, kitchen(""));
        assertThat(json(theirReport, "$.preparations")).isEqualTo("5");
        assertThat(read(theirReport, "$.slowest[*].name").toString()).isEqualTo("[\"Pizza grande\"]");
    }

    @Test
    void theSectorFilterCountsOnlyTheTablesOfTheSector() throws Exception {
        Place deck = data.place(sakura, "Deck");
        UUID guioza = jdbc.queryForObject("SELECT id FROM product WHERE establishment_id = ? AND name = 'Guioza'",
                UUID.class, sakura.establishmentId());
        data.kitchenOrder(sakura, deck, at(day, 19, 0), null, prep(guioza, "Guioza", 30));

        MvcTestResult onDeck = data.get(sakura, kitchen("&sectorId=" + deck.sector()));
        MvcTestResult inSalao = data.get(sakura, kitchen("&sectorId=" + salao.sector()));

        assertThat(json(onDeck, "$.preparations")).isEqualTo("1");
        assertThat(json(onDeck, "$.averagePrepSeconds")).isEqualTo("1800");
        assertThat(json(inSalao, "$.preparations")).isEqualTo("15");
    }

    @Test
    void onlyTheOwnerAndTheManagerWithReportsInThePlan() {
        assertThat(data.get(data.token(sakura, Role.MANAGER), kitchen(""))).hasStatusOk();
        for (Role role : new Role[]{Role.STAFF, Role.KITCHEN}) {
            assertThat(data.get(data.token(sakura, role), kitchen(""))).hasStatus(HttpStatus.FORBIDDEN);
        }
        data.onPlan(sakura, "essential");
        assertThat(data.get(sakura, kitchen(""))).hasStatus(HttpStatus.FORBIDDEN);
    }
}
