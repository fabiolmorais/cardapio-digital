package br.com.cardapio.adapter.in.web.report;

import br.com.cardapio.adapter.in.web.report.ReportTestData.Owner;
import br.com.cardapio.adapter.in.web.report.ReportTestData.Place;
import br.com.cardapio.application.port.out.security.AccessTokenIssuer;
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

/** O Cardápio completo (R7): curva ABC, variação, cancelados, tamanhos, opções, pedidos juntos e vídeo. */
@IntegrationTest
class MenuFullReportControllerTest {

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
    private UUID guioza;

    /**
     * Na semana: guioza 4 (R$ 116), chope 5 (R$ 75: 2 de 300 ml e 3 de 500 ml), yakissoba 1 (R$ 49, mais
     * 2 cancelados), chá 2 (R$ 18) e pastel 1 (R$ 3), em 3 comandas; guioza e chope nas 3. Um molho extra
     * de R$ 2 e um gengibre grátis no guioza; o guioza tem vídeo. Na semana anterior: 1 guioza.
     */
    @BeforeEach
    void sakuraWithAWeek() throws Exception {
        data = new ReportTestData(mvc, jdbc, tokenIssuer);
        sakura = data.newRestaurant();
        Place salao = data.place(sakura, "Salão");
        Instant old = Instant.now().minus(Duration.ofDays(60));
        UUID pratos = data.category(sakura, "Pratos");
        UUID bebidas = data.category(sakura, "Bebidas");
        guioza = data.product(sakura, pratos, "Guioza", 2900, old);
        UUID yakissoba = data.product(sakura, pratos, "Yakissoba", 4900, old);
        UUID pastel = data.product(sakura, pratos, "Pastel", 300, old);
        UUID chopp = data.product(sakura, bebidas, "Chopp", 1500, old);
        UUID cha = data.product(sakura, bebidas, "Chá", 900, old);

        UUID t1 = data.closedTab(sakura, salao, at(day, 20, 0), at(day, 21, 0), 5900, "PIX", 0,
                item(guioza, "Guioza", 2900, 1), item(chopp, "Chopp", 1500, 2), canceled(yakissoba, "Yakissoba", 4900, 2));
        UUID t2 = data.closedTab(sakura, salao, at(day, 21, 0), at(day, 22, 0), 9300, "PIX", 0,
                item(guioza, "Guioza", 2900, 1), item(chopp, "Chopp", 1500, 1), item(yakissoba, "Yakissoba", 4900, 1));
        UUID t3 = data.closedTab(sakura, salao, at(first, 12, 0), at(first, 13, 0), 10600, "PIX", 0,
                item(guioza, "Guioza", 2900, 2), item(chopp, "Chopp", 1500, 2), item(cha, "Chá", 900, 2),
                item(pastel, "Pastel", 300, 1));
        data.closedTab(sakura, salao, at(first.minusDays(3), 12, 0), at(first.minusDays(3), 13, 0), 2900, "PIX", 0,
                item(guioza, "Guioza", 2900, 1));
        jdbc.update("""
                UPDATE order_item SET variant_name = CASE WHEN order_id IN (SELECT id FROM customer_order WHERE tab_id = ?)
                                                          THEN '300 ml' ELSE '500 ml' END
                WHERE product_id = ?
                """, t1, chopp);

        UUID group = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO option_group (id, establishment_id, name, title, min_choices, max_choices, max_per_option,
                                          pricing)
                VALUES (?, ?, 'Molhos', 'Molhos', 0, 3, 1, 'SUM')
                """, group, sakura.establishmentId());
        UUID guiozaItem = jdbc.queryForObject("""
                SELECT i.id FROM order_item i JOIN customer_order o ON o.id = i.order_id
                WHERE o.tab_id = ? AND i.product_id = ?
                """, UUID.class, t1, guioza);
        option(group, guiozaItem, "Molho extra", 200, 200);
        option(group, guiozaItem, "Gengibre", 100, 0);

        jdbc.update("""
                INSERT INTO product_media (id, establishment_id, product_id, type, storage_path, width, height, position,
                                           video_id, status, duration_seconds)
                VALUES (?, ?, ?, 'VIDEO', 'video', 1080, 1920, 0, ?, 'READY', 15)
                """, UUID.randomUUID(), sakura.establishmentId(), guioza, UUID.randomUUID().toString());
        assertThat(t2).isNotNull();
        assertThat(t3).isNotNull();
    }

    private void option(UUID group, UUID orderItem, String name, long price, long charged) {
        UUID option = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO option_item (id, establishment_id, option_group_id, name, price_cents, position)
                VALUES (?, ?, ?, ?, ?, 0)
                """, option, sakura.establishmentId(), group, name, price);
        jdbc.update("""
                INSERT INTO order_item_option (id, establishment_id, order_item_id, option_group_id, option_item_id,
                                               group_title, option_name, splits_item, quantity, price_cents,
                                               charged_cents, position)
                VALUES (?, ?, ?, ?, ?, 'Molhos', ?, false, 1, ?, ?, 0)
                """, UUID.randomUUID(), sakura.establishmentId(), orderItem, group, option, name, price, charged);
    }

    private String products(String extra) {
        return "/api/v1/reports/menu/products?from=%s&to=%s%s".formatted(first, day, extra);
    }

    private String menu() {
        return "/api/v1/reports/menu?from=%s&to=%s".formatted(first, day);
    }

    @Test
    void theProSeesTheTableWithoutTheBusinessColumns() throws Exception {
        MvcTestResult table = data.get(sakura, products(""));

        assertThat(json(table, "$.content[0].name")).isEqualTo("Guioza");
        assertThat(json(table, "$.content[0].abc")).isNull();
        assertThat(json(table, "$.content[0].comparedRevenueCents")).isNull();
        assertThat(json(table, "$.content[0].canceled")).isNull();
        assertThat(json(data.get(sakura, menu()), "$.full")).isNull();
        // Ordenar pelo crescimento é do Business.
        assertThat(data.get(sakura, products("&sort=GROWTH"))).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void theAbcCurveTheVariationAndTheCanceled() throws Exception {
        data.onPlan(sakura, "business");

        MvcTestResult table = data.get(sakura, products(""));

        List<String> names = read(table, "$.content[*].name");
        assertThat(names).containsExactly("Guioza", "Chopp", "Yakissoba", "Chá", "Pastel");
        List<String> abc = read(table, "$.content[*].abc");
        // R$ 261 no total: até R$ 191 (73%), A; o chá começa em R$ 240 (92%), B; o pastel em 98,9%, C.
        assertThat(abc).containsExactly("A", "A", "A", "B", "C");
        assertThat(json(table, "$.content[0].comparedRevenueCents")).isEqualTo("2900");
        assertThat(json(table, "$.content[1].comparedRevenueCents")).isEqualTo("0");
        assertThat(json(table, "$.content[2].canceled")).isEqualTo("2");
        // Pelo crescimento: o guioza (de R$ 29 para R$ 116); quem não vendeu antes vai para o fim.
        assertThat(json(data.get(sakura, products("&sort=GROWTH")), "$.content[0].name")).isEqualTo("Guioza");
    }

    @Test
    void theBusinessBlocks() throws Exception {
        data.onPlan(sakura, "business");

        MvcTestResult result = data.get(sakura, menu());

        assertThat(json(result, "$.full.mostCanceled[0].name")).isEqualTo("Yakissoba");
        assertThat(json(result, "$.full.mostCanceled[0].times")).isEqualTo("2");
        assertThat(json(result, "$.full.mostCanceled[0].reason")).isEqualTo("Cliente desistiu");
        assertThat(json(result, "$.full.sizes[0].product")).isEqualTo("Chopp");
        assertThat(read(result, "$.full.sizes[0].sizes[*].name").toString()).isEqualTo("[\"500 ml\",\"300 ml\"]");
        assertThat(read(result, "$.full.options[*].name").toString()).isEqualTo("[\"Molho extra\",\"Gengibre\"]");
        assertThat(json(result, "$.full.options[1].priced")).isEqualTo("true");
        assertThat(json(result, "$.full.options[1].chargedCents")).isEqualTo("0");
        assertThat(json(result, "$.full.paidOptionsCents")).isEqualTo("200");
        assertThat(json(result, "$.full.pairs[0].first")).isIn("Guioza", "Chopp");
        assertThat(json(result, "$.full.pairs[0].together")).isEqualTo("3");
        assertThat(json(result, "$.full.pairs[0].firstTabs")).isEqualTo("3");
        assertThat(json(result, "$.full.video.withVideo")).isEqualTo("1");
        assertThat(json(result, "$.full.video.withVideoSold")).isEqualTo("4");
        assertThat(json(result, "$.full.video.withoutVideo")).isEqualTo("4");
        assertThat(json(result, "$.full.video.withoutVideoSold")).isEqualTo("9");
    }

    @Test
    void anotherRestaurantNeverShowsUp() throws Exception {
        data.onPlan(sakura, "business");
        Owner other = data.newRestaurant();
        data.onPlan(other, "business");
        Place theirs = data.place(other, "Salão");
        UUID pizza = data.product(other, "Pizza", 5000);
        data.closedTab(other, theirs, at(day, 20, 0), at(day, 21, 0), 5000, "PIX", 0, item(pizza, "Pizza", 5000, 1),
                canceled(pizza, "Pizza", 5000, 3));

        MvcTestResult mine = data.get(sakura, menu());
        MvcTestResult myTable = data.get(sakura, products(""));
        MvcTestResult theirs2 = data.get(other, menu());

        assertThat(read(mine, "$.full.mostCanceled[*].name").toString()).doesNotContain("Pizza");
        assertThat(read(myTable, "$.content[*].name").toString()).doesNotContain("Pizza");
        assertThat(json(theirs2, "$.full.mostCanceled[0].name")).isEqualTo("Pizza");
        assertThat(read(theirs2, "$.full.options").toString()).isEqualTo("[]");
        assertThat(json(theirs2, "$.full.video.withVideo")).isEqualTo("0");
    }
}
