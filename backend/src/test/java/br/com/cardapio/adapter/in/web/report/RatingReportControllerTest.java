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

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static br.com.cardapio.adapter.in.web.report.ReportTestData.SAO_PAULO;
import static br.com.cardapio.adapter.in.web.report.ReportTestData.at;
import static br.com.cardapio.adapter.in.web.report.ReportTestData.item;
import static br.com.cardapio.adapter.in.web.report.ReportTestData.json;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * As avaliações no Atendimento e equipe (C6; REL-55, 83, 84): a nota por garçom, o resumo, a evolução,
 * os turnos, os atalhos e os comentários.
 */
@IntegrationTest
class RatingReportControllerTest {

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
    private UUID varanda;

    /**
     * Na semana: o Carlos com 5 avaliações no jantar do último dia (5, 5, 4, 5, 4; uma foi ao Google, uma
     * com comentário); a Ana com 2 no almoço (2 no último dia e 3 no primeiro, com comentário e "Rapidez");
     * a Varanda com uma nota 1 sem garçom, comentada. Antes da semana, um 3. Duas comandas fechadas.
     */
    @BeforeEach
    void sakuraWithAWeekOfRatings() throws Exception {
        data = new ReportTestData(mvc, jdbc, tokenIssuer);
        sakura = data.newRestaurant();
        Place salao = data.place(sakura, "Salão");
        Place deck = data.place(sakura, "Varanda");
        varanda = deck.sector();
        UUID carlos = data.waiter(sakura, "Carlos");
        UUID ana = data.waiter(sakura, "Ana");
        UUID prato = data.product(sakura, "Prato da casa", 2500);
        data.closedTab(sakura, salao, at(day, 19, 0), at(day, 21, 0), 2500, "PIX", 0, item(prato, "Prato da casa", 2500, 1));
        data.closedTab(sakura, salao, at(day, 12, 0), at(day, 13, 0), 2500, "PIX", 0, item(prato, "Prato da casa", 2500, 1));

        int[] carlosStars = {5, 5, 4, 5, 4};
        for (int i = 0; i < carlosStars.length; i++) {
            UUID id = data.rating(sakura, salao, carlos, carlosStars[i], i == 0 ? "O Carlos explicou tudo" : null,
                    at(day, 20, i), "FRIENDLINESS", i < 2 ? "FOOD" : "AMBIENCE");
            if (i == 0) {
                jdbc.update("UPDATE service_rating SET google_clicked_at = created_at WHERE id = ?", id);
            }
        }
        data.rating(sakura, salao, ana, 2, "A conta demorou", at(day, 12, 30), "SPEED");
        data.rating(sakura, salao, ana, 3, "Comida boa, cozinha lenta", at(first, 13, 0), "SPEED", "FOOD");
        data.rating(sakura, deck, null, 1, "Mesa da varanda esquecida", at(day, 21, 0));
        data.rating(sakura, salao, carlos, 3, null, at(first.minusDays(3), 20, 0));
    }

    private String uri(String path, String extra) {
        return "/api/v1/reports/team%s?from=%s&to=%s%s".formatted(path, first, day, extra);
    }

    @Test
    void theProDoesNotSeeIt() throws Exception {
        assertThat(data.get(sakura, uri("", ""))).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(data.get(sakura, uri("/comments", ""))).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void theWaitersGetTheirNoteWithFiveRatingsOrMore() throws Exception {
        data.onPlan(sakura, "business");

        MvcTestResult r = data.get(sakura, uri("", ""));

        assertThat(json(r, "$.waiters[?(@.name == 'Carlos')].ratings")).isEqualTo("[5]");
        assertThat(json(r, "$.waiters[?(@.name == 'Carlos')].ratingAverage")).isEqualTo("[4.6]");
        assertThat(json(r, "$.waiters[?(@.name == 'Ana')].ratings")).isEqualTo("[2]");
        assertThat(json(r, "$.waiters[?(@.name == 'Ana')].ratingAverage")).isEqualTo("[null]");
    }

    @Test
    void theRatingsBlock() throws Exception {
        data.onPlan(sakura, "business");

        MvcTestResult r = data.get(sakura, uri("", ""));

        // 8 avaliações, 29 estrelas: 3,6.
        assertThat(json(r, "$.ratings.count")).isEqualTo("8");
        assertThat(json(r, "$.ratings.average")).isEqualTo("3.6");
        assertThat(json(r, "$.ratings.comparedAverage")).isEqualTo("3.0");
        assertThat(json(r, "$.ratings.low")).isEqualTo("2");
        assertThat(json(r, "$.ratings.high")).isEqualTo("5");
        assertThat(json(r, "$.ratings.googleClicks")).isEqualTo("1");
        assertThat(json(r, "$.ratings.closedTabs")).isEqualTo("2");
        assertThat(ReportTestData.<List<Object>>read(r, "$.ratings.byStars")).isEqualTo(List.of(1, 1, 1, 2, 3));
        assertThat(json(r, "$.ratings.lunch.count")).isEqualTo("2");
        assertThat(json(r, "$.ratings.lunch.average")).isEqualTo("2.5");
        assertThat(json(r, "$.ratings.dinner.average")).isEqualTo("4.0");
        assertThat(json(r, "$.ratings.praisedTags[0].tag")).isEqualTo("FRIENDLINESS");
        assertThat(json(r, "$.ratings.praisedTags[0].times")).isEqualTo("5");
        assertThat(json(r, "$.ratings.improveTags[0].tag")).isEqualTo("SPEED");
        assertThat(json(r, "$.ratings.improveTags[0].times")).isEqualTo("2");
        // 7 dias: um ponto por dia; o primeiro com o 3 da Ana, o último com 7 avaliações (26 estrelas).
        assertThat(ReportTestData.<List<Object>>read(r, "$.ratings.trend")).hasSize(7);
        assertThat(json(r, "$.ratings.trend[0].average")).isEqualTo("3.0");
        assertThat(json(r, "$.ratings.trend[6].count")).isEqualTo("7");
        assertThat(json(r, "$.ratings.trend[6].average")).isEqualTo("3.7");
        assertThat(json(r, "$.ratings.trend[3].average")).isNull();
    }

    @Test
    void theCommentsComeNewestFirstPagedAndFiltered() throws Exception {
        data.onPlan(sakura, "business");

        MvcTestResult all = data.get(sakura, uri("/comments", "&size=2"));
        assertThat(json(all, "$.totalElements")).isEqualTo("4");
        assertThat(json(all, "$.content[0].comment")).isEqualTo("Mesa da varanda esquecida");
        assertThat(json(all, "$.content[1].waiterName")).isEqualTo("Carlos");

        MvcTestResult low = data.get(sakura, uri("/comments", "&onlyLow=true"));
        assertThat(ReportTestData.<List<Object>>read(low, "$.content[*].stars")).isEqualTo(List.of(1, 2));
        assertThat(json(low, "$.content[1].tags[0]")).isEqualTo("SPEED");
    }

    @Test
    void theSectorFilters() throws Exception {
        data.onPlan(sakura, "business");

        MvcTestResult deck = data.get(sakura, uri("", "&sectorId=" + varanda));

        assertThat(json(deck, "$.ratings.count")).isEqualTo("1");
        assertThat(json(data.get(sakura, uri("/comments", "&sectorId=" + varanda)), "$.totalElements"))
                .isEqualTo("1");
    }

    @Test
    void theExportCarriesTheRatingsAndTheComments() throws Exception {
        data.onPlan(sakura, "business");

        String csv = data.get(sakura, uri("/export", "")).getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(csv).contains("Avaliações;Nota", "Nota média;3,6", "Comentários",
                "Mesa da varanda esquecida", "Rapidez");
    }

    @Test
    void anotherRestaurantNeverShowsUp() throws Exception {
        data.onPlan(sakura, "business");
        Owner other = data.newRestaurant();
        data.onPlan(other, "business");
        Place theirs = data.place(other, "Salão");
        data.rating(other, theirs, null, 5, "Segredo do outro", at(day, 20, 0));

        assertThat(data.get(sakura, uri("/comments", "")).getResponse().getContentAsString())
                .doesNotContain("Segredo do outro");
        assertThat(json(data.get(sakura, uri("", "")), "$.ratings.count")).isEqualTo("8");
        assertThat(json(data.get(other, uri("", "")), "$.ratings.count")).isEqualTo("1");
        assertThat(data.get(other, uri("/comments", "")).getResponse().getContentAsString())
                .doesNotContain("Carlos");
        assertThat(data.get(other, uri("", "&sectorId=" + varanda))).hasStatus(HttpStatus.NOT_FOUND);
    }
}
