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

/** O Cardápio (R2): por categoria, a tabela de produtos paginada e os que vendem pouco. */
@IntegrationTest
class MenuReportControllerTest {

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
    private UUID prato;
    private UUID costela;
    private UUID chopp;

    /**
     * Pratos: prato da casa e costela. Bebidas: chopp, água (nada vendido) e suco (novo, de 5 dias).
     * Na semana: o chopp vende 6 (R$ 90,00), a costela 1 (R$ 79,90) e o prato 2 (R$ 50,00), mais um prato
     * cancelado e uma comanda aberta que não contam.
     */
    @BeforeEach
    void sakuraWithAMenuAndAWeekOfSales() throws Exception {
        data = new ReportTestData(mvc, jdbc, tokenIssuer);
        sakura = data.newRestaurant();
        salao = data.place(sakura, "Salão");
        Instant twoMonthsAgo = Instant.now().minus(Duration.ofDays(60));
        UUID pratos = data.category(sakura, "Pratos");
        bebidas = data.category(sakura, "Bebidas");
        prato = data.product(sakura, pratos, "Prato da casa", 2500, twoMonthsAgo);
        costela = data.product(sakura, pratos, "Costela no bafo", 7990, twoMonthsAgo);
        chopp = data.product(sakura, bebidas, "Chopp 500 ml", 1500, twoMonthsAgo);
        data.product(sakura, bebidas, "Água mineral", 490, twoMonthsAgo);
        data.product(sakura, bebidas, "Suco detox", 1490, Instant.now().minus(Duration.ofDays(5)));

        data.closedTab(sakura, salao, at(first, 20, 0), at(first, 21, 0), 11000, "PIX", 0,
                item(prato, "Prato da casa", 2500, 2), item(chopp, "Chopp 500 ml", 1500, 4));
        data.closedTab(sakura, salao, at(day, 20, 0), at(day, 21, 0), 10990, "CARD", 0,
                item(costela, "Costela no bafo", 7990, 1), item(chopp, "Chopp 500 ml", 1500, 2),
                canceled(prato, "Prato da casa", 2500, 1));
        data.openTab(sakura, salao, at(day, 22, 0), item(costela, "Costela no bafo", 7990, 5));
    }

    private String menu(String extra) {
        return "/api/v1/reports/menu?from=%s&to=%s%s".formatted(first, day, extra);
    }

    private String products(String extra) {
        return "/api/v1/reports/menu/products?from=%s&to=%s%s".formatted(first, day, extra);
    }

    @Test
    void byCategoryWithTheTotalsOfTheItemsSold() throws Exception {
        MvcTestResult result = data.get(sakura, menu(""));

        assertThat(result).hasStatusOk();
        assertThat(json(result, "$.level")).isEqualTo("BASIC");
        assertThat(json(result, "$.itemsRevenueCents")).isEqualTo("21990");
        assertThat(json(result, "$.itemsSold")).isEqualTo("9");
        assertThat(json(result, "$.activeProducts")).isEqualTo("5");
        assertThat(read(result, "$.byCategory[*].name").toString()).isEqualTo("[\"Pratos\",\"Bebidas\"]");
        assertThat(json(result, "$.byCategory[0].revenueCents")).isEqualTo("12990");
        assertThat(json(result, "$.byCategory[0].quantity")).isEqualTo("3");
        assertThat(json(result, "$.byCategory[1].revenueCents")).isEqualTo("9000");
    }

    @Test
    void theProductTableSortsByRevenueOrQuantityAndIsPaginated() throws Exception {
        MvcTestResult byRevenue = data.get(sakura, products(""));
        MvcTestResult byQuantity = data.get(sakura, products("&sort=QUANTITY"));

        assertThat(read(byRevenue, "$.content[*].name").toString())
                .isEqualTo("[\"Chopp 500 ml\",\"Costela no bafo\",\"Prato da casa\"]");
        assertThat(json(byRevenue, "$.content[0].category")).isEqualTo("Bebidas");
        assertThat(json(byRevenue, "$.content[0].quantity")).isEqualTo("6");
        assertThat(json(byRevenue, "$.content[0].revenueCents")).isEqualTo("9000");
        assertThat(read(byQuantity, "$.content[*].name").toString())
                .isEqualTo("[\"Chopp 500 ml\",\"Prato da casa\",\"Costela no bafo\"]");

        MvcTestResult firstPage = data.get(sakura, products("&size=2"));
        MvcTestResult secondPage = data.get(sakura, products("&size=2&page=1"));
        assertThat(json(firstPage, "$.totalElements")).isEqualTo("3");
        assertThat(json(firstPage, "$.totalPages")).isEqualTo("2");
        assertThat(read(firstPage, "$.content[*].name").toString()).isEqualTo("[\"Chopp 500 ml\",\"Costela no bafo\"]");
        assertThat(read(secondPage, "$.content[*].name").toString()).isEqualTo("[\"Prato da casa\"]");
        assertThat(data.get(sakura, products("&size=101"))).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(data.get(sakura, products("&page=-1"))).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void theCategoryFilterNarrowsEverything() throws Exception {
        MvcTestResult result = data.get(sakura, menu("&categoryId=" + bebidas));
        MvcTestResult table = data.get(sakura, products("&categoryId=" + bebidas));

        assertThat(read(result, "$.byCategory[*].name").toString()).isEqualTo("[\"Bebidas\"]");
        assertThat(json(result, "$.activeProducts")).isEqualTo("3");
        assertThat(read(table, "$.content[*].name").toString()).isEqualTo("[\"Chopp 500 ml\"]");
        // A água não vendeu nada; o suco é novo e fica de fora.
        assertThat(read(result, "$.slowSellers[*].name").toString()).isEqualTo("[\"Água mineral\",\"Chopp 500 ml\"]");
    }

    @Test
    void slowSellersIncludeWhatSoldNothingAndLeaveOutNewProducts() throws Exception {
        MvcTestResult result = data.get(sakura, menu(""));

        List<String> names = read(result, "$.slowSellers[*].name");
        assertThat(names).containsExactly("Água mineral", "Costela no bafo", "Prato da casa", "Chopp 500 ml");
        assertThat(json(result, "$.slowSellers[0].quantity")).isEqualTo("0");
        assertThat(json(result, "$.slowSellers[0].revenueCents")).isEqualTo("0");
    }

    @Test
    void anotherRestaurantsMenuNeverShowsUp() throws Exception {
        Owner other = data.newRestaurant();
        Place theirs = data.place(other, "Salão");
        UUID theirCategory = data.category(other, "Pizzas");
        UUID pizza = data.product(other, theirCategory, "Pizza grande", 7490, Instant.now().minus(Duration.ofDays(60)));
        data.closedTab(other, theirs, at(day, 20, 0), at(day, 21, 0), 7490, "PIX", 0, item(pizza, "Pizza grande", 7490, 1));

        MvcTestResult mine = data.get(sakura, menu(""));
        MvcTestResult myProducts = data.get(sakura, products(""));

        assertThat(read(mine, "$.byCategory[*].name").toString()).doesNotContain("Pizzas");
        assertThat(read(mine, "$.slowSellers[*].name").toString()).doesNotContain("Pizza");
        assertThat(read(myProducts, "$.content[*].name").toString()).doesNotContain("Pizza");
        assertThat(json(mine, "$.activeProducts")).isEqualTo("5");
        // A categoria de outro restaurante: 404, sem revelar que existe.
        assertThat(data.get(sakura, menu("&categoryId=" + theirCategory))).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(data.get(sakura, products("&categoryId=" + theirCategory))).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(data.get(sakura, products("&sectorId=" + theirs.sector()))).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void onlyTheOwnerAndTheManagerWithReportsInThePlan() {
        assertThat(data.get(data.token(sakura, Role.MANAGER), menu(""))).hasStatusOk();
        for (Role role : new Role[]{Role.STAFF, Role.KITCHEN}) {
            assertThat(data.get(data.token(sakura, role), menu(""))).hasStatus(HttpStatus.FORBIDDEN);
            assertThat(data.get(data.token(sakura, role), products(""))).hasStatus(HttpStatus.FORBIDDEN);
        }
        data.onPlan(sakura, "essential");
        assertThat(data.get(sakura, menu(""))).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(data.get(sakura, products(""))).hasStatus(HttpStatus.FORBIDDEN);
    }
}
