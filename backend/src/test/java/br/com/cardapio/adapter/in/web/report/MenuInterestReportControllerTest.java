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
import static br.com.cardapio.adapter.in.web.report.ReportTestData.item;
import static br.com.cardapio.adapter.in.web.report.ReportTestData.json;
import static br.com.cardapio.adapter.in.web.report.ReportTestData.read;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Visto x pedido, reels e carrinhos no Cardápio (C3; REL-27, 28, 29): os eventos do celular cruzados com
 * os pedidos da mesma sessão.
 */
@IntegrationTest
class MenuInterestReportControllerTest {

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
    private UUID bebidas;

    /**
     * Três celulares na semana: o 1 (Salão) vê e põe o guioza no carrinho, pede, e vê o lámen; o 2 (Salão)
     * vê o reel do guioza e põe o lámen no carrinho, e não pede nada; o 3 (Varanda) abre o guioza duas
     * vezes, põe 3 chás no carrinho e pede só o guioza. Um lámen visto antes da semana marca o começo da
     * gravação.
     */
    @BeforeEach
    void sakuraWithAWeekOfPhones() throws Exception {
        data = new ReportTestData(mvc, jdbc, tokenIssuer);
        sakura = data.newRestaurant();
        Place salao = data.place(sakura, "Salão");
        Place varanda = data.place(sakura, "Varanda");
        Instant old = Instant.now().minus(Duration.ofDays(60));
        UUID pratos = data.category(sakura, "Pratos");
        bebidas = data.category(sakura, "Bebidas");
        UUID guioza = data.product(sakura, pratos, "Guioza", 2900, old);
        UUID lamen = data.product(sakura, pratos, "Lámen", 3500, old);
        UUID cha = data.product(sakura, bebidas, "Chá", 900, old);

        Instant night = at(day, 20, 0);
        UUID one = data.session(sakura, salao, night);
        data.menuEvent(sakura, one, guioza, "PRODUCT_VIEW", 1, night);
        data.menuEvent(sakura, one, guioza, "REEL_VIEW", 1, night);
        data.menuEvent(sakura, one, guioza, "CART_ADD", 2, night);
        data.menuEvent(sakura, one, lamen, "PRODUCT_VIEW", 1, night);
        UUID salaoTab = data.openTab(sakura, salao, night);
        data.phoneOrder(sakura, salao, salaoTab, one, night.plusSeconds(300), item(guioza, "Guioza", 2900, 2));

        UUID two = data.session(sakura, salao, night);
        data.menuEvent(sakura, two, guioza, "REEL_VIEW", 1, night);
        data.menuEvent(sakura, two, lamen, "PRODUCT_VIEW", 1, night);
        data.menuEvent(sakura, two, lamen, "CART_ADD", 1, night);

        UUID three = data.session(sakura, varanda, night);
        data.menuEvent(sakura, three, guioza, "PRODUCT_VIEW", 1, night);
        data.menuEvent(sakura, three, guioza, "PRODUCT_VIEW", 1, night.plusSeconds(60));
        data.menuEvent(sakura, three, cha, "CART_ADD", 3, night);
        UUID varandaTab = data.openTab(sakura, varanda, night);
        data.phoneOrder(sakura, varanda, varandaTab, three, night.plusSeconds(300), item(guioza, "Guioza", 2900, 1));

        UUID before = data.session(sakura, salao, at(first.minusDays(3), 20, 0));
        data.menuEvent(sakura, before, lamen, "PRODUCT_VIEW", 1, at(first.minusDays(3), 20, 0));
    }

    private String report(String extra) {
        return "/api/v1/reports/menu?from=%s&to=%s%s".formatted(first, day, extra);
    }

    @Test
    void theProDoesNotReceiveIt() throws Exception {
        data.onPlan(sakura, "pro");

        assertThat(data.get(sakura, report("")).getResponse().getContentAsString()).doesNotContain("interest");
    }

    @Test
    void viewedVersusOrderedReelsAndCarts() throws Exception {
        data.onPlan(sakura, "business");

        MvcTestResult r = data.get(sakura, report(""));

        assertThat(json(r, "$.full.interest.recordingSince")).isEqualTo(first.minusDays(3).toString());
        assertThat(ReportTestData.<List<Object>>read(r, "$.full.interest.viewed[*].name")).isEqualTo(List.of("Guioza", "Lámen"));
        assertThat(ReportTestData.<List<Object>>read(r, "$.full.interest.viewed[*].viewers")).isEqualTo(List.of(3, 2));
        assertThat(ReportTestData.<List<Object>>read(r, "$.full.interest.viewed[*].ordered")).isEqualTo(List.of(2, 0));
        assertThat(ReportTestData.<List<Object>>read(r, "$.full.interest.reels[*].name")).isEqualTo(List.of("Guioza"));
        assertThat(json(r, "$.full.interest.reels[0].viewers")).isEqualTo("2");
        assertThat(json(r, "$.full.interest.reels[0].ordered")).isEqualTo("1");
        assertThat(json(r, "$.full.interest.cartSessions")).isEqualTo("3");
        assertThat(json(r, "$.full.interest.cartsNotSent")).isEqualTo("1");
        // Ficaram 3 chás (R$ 27) e 1 lámen (R$ 35); o guioza do carrinho foi pedido.
        assertThat(json(r, "$.full.interest.leftCents")).isEqualTo("6200");
        assertThat(ReportTestData.<List<Object>>read(r, "$.full.interest.mostLeft[*].name")).isEqualTo(List.of("Chá", "Lámen"));
        assertThat(ReportTestData.<List<Object>>read(r, "$.full.interest.mostLeft[*].units")).isEqualTo(List.of(3, 1));
    }

    @Test
    void theSectorAndTheCategoryFilter() throws Exception {
        data.onPlan(sakura, "business");
        UUID varanda = jdbc.queryForObject("SELECT id FROM sector WHERE establishment_id = ? AND name = 'Varanda'",
                UUID.class, sakura.establishmentId());

        MvcTestResult sector = data.get(sakura, report("&sectorId=" + varanda));
        assertThat(ReportTestData.<List<Object>>read(sector, "$.full.interest.viewed[*].name")).isEqualTo(List.of("Guioza"));
        assertThat(ReportTestData.<List<Object>>read(sector, "$.full.interest.viewed[*].ordered")).isEqualTo(List.of(1));
        assertThat(json(sector, "$.full.interest.cartSessions")).isEqualTo("1");
        assertThat(json(sector, "$.full.interest.cartsNotSent")).isEqualTo("0");

        MvcTestResult drinks = data.get(sakura, report("&categoryId=" + bebidas));
        assertThat(ReportTestData.<List<Object>>read(drinks, "$.full.interest.viewed")).isEqualTo(List.of());
        assertThat(ReportTestData.<List<Object>>read(drinks, "$.full.interest.mostLeft[*].name")).isEqualTo(List.of("Chá"));
        assertThat(json(drinks, "$.full.interest.leftCents")).isEqualTo("2700");
    }

    @Test
    void anotherRestaurantNeverShowsUp() throws Exception {
        data.onPlan(sakura, "business");
        Owner other = data.newRestaurant();
        data.onPlan(other, "business");
        Place theirs = data.place(other, "Salão");
        UUID dish = data.product(other, "Udon", 3200);
        UUID phone = data.session(other, theirs, at(day, 20, 0));
        data.menuEvent(other, phone, dish, "PRODUCT_VIEW", 1, at(day, 20, 0));
        data.menuEvent(other, phone, dish, "CART_ADD", 4, at(day, 20, 0));

        MvcTestResult ours = data.get(sakura, report(""));
        assertThat(ours.getResponse().getContentAsString()).doesNotContain("Udon");
        assertThat(json(ours, "$.full.interest.cartSessions")).isEqualTo("3");

        MvcTestResult theirsReport = data.get(other, report(""));
        assertThat(ReportTestData.<List<Object>>read(theirsReport, "$.full.interest.viewed[*].name")).isEqualTo(List.of("Udon"));
        assertThat(json(theirsReport, "$.full.interest.cartsNotSent")).isEqualTo("1");
        assertThat(json(theirsReport, "$.full.interest.leftCents")).isEqualTo("12800");
    }
}
