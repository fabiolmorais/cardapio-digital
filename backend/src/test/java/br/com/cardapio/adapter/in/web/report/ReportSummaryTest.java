package br.com.cardapio.adapter.in.web.report;

import br.com.cardapio.adapter.in.web.report.ReportTestData.Owner;
import br.com.cardapio.adapter.in.web.report.ReportTestData.Place;
import br.com.cardapio.application.port.in.report.RefreshReportSummaryUseCase;
import br.com.cardapio.application.port.out.security.AccessTokenIssuer;
import br.com.cardapio.domain.report.model.SummaryStatus;
import br.com.cardapio.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static br.com.cardapio.adapter.in.web.report.ReportTestData.SAO_PAULO;
import static br.com.cardapio.adapter.in.web.report.ReportTestData.at;
import static br.com.cardapio.adapter.in.web.report.ReportTestData.canceled;
import static br.com.cardapio.adapter.in.web.report.ReportTestData.item;
import static br.com.cardapio.adapter.in.web.report.ReportTestData.json;
import static br.com.cardapio.adapter.in.web.report.ReportTestData.prep;
import static br.com.cardapio.adapter.in.web.report.ReportTestData.read;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * O resumo diário dos relatórios (C9, ADR 0014): os números com o resumo são os mesmos das consultas
 * diretas, a troca de setor de uma mesa apaga o resumo, e o resumo de um restaurante nunca aparece
 * em outro.
 */
@IntegrationTest
class ReportSummaryTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    AccessTokenIssuer tokenIssuer;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    RefreshReportSummaryUseCase summaries;

    private final LocalDate today = LocalDate.now(SAO_PAULO);

    private ReportTestData data;
    private Owner sakura;
    private Place salao;
    private Place varanda;

    /**
     * 70 dias de movimento em dois setores: comandas fechadas (com item cancelado), pedidos na cozinha
     * com tempos diferentes (um deles atrasando nos últimos dias) e um pouco de hoje, que nunca entra no
     * resumo.
     */
    @BeforeEach
    void seventyDaysInTwoSectors() throws Exception {
        data = new ReportTestData(mvc, jdbc, tokenIssuer);
        sakura = data.newRestaurant();
        data.onPlan(sakura, "business");
        salao = data.place(sakura, "Salão");
        varanda = data.place(sakura, "Varanda");
        UUID guioza = data.product(sakura, "Guioza", 2990);
        UUID temaki = data.product(sakura, "Temaki", 3500);
        UUID cha = data.product(sakura, "Chá verde", 800);
        for (int back = 70; back >= 1; back--) {
            LocalDate day = today.minusDays(back);
            Place place = back % 3 == 0 ? varanda : salao;
            data.closedTab(sakura, place, at(day, 19, 0), at(day, 21, back % 50), 2990 * 2 + 800, "PIX", 0,
                    item(guioza, "Guioza", 2990, 2), item(cha, "Chá verde", 800, 1), canceled(temaki, "Temaki", 3500, 1));
            if (back % 4 == 0) {
                data.closedTab(sakura, salao, at(day, 12, 0), at(day, 13, 0), 3500 * 3, "CARD", 0,
                        item(temaki, "Temaki", 3500, 3));
            }
            // O Guioza demora mais nos últimos 10 dias (vira ponto de atenção).
            data.kitchenOrder(sakura, place, at(day, 19, 10), back % 5 == 0 ? at(day, 19, 12) : null,
                    prep(guioza, "Guioza", back <= 10 ? 30 : 12 + back % 3), prep(temaki, "Temaki", 8 + back % 4),
                    prep(cha, "Chá verde", 2));
        }
        // Hoje cedo: entra pelas consultas diretas.
        data.closedTab(sakura, salao, at(today, 0, 5), at(today, 0, 20), 3500, "PIX", 0,
                item(temaki, "Temaki", 3500, 1));
    }

    private String overview(String extra) {
        return "/api/v1/reports/overview?from=%s&to=%s%s".formatted(today.minusDays(69), today, extra);
    }

    private String blocks(MvcTestResult result) throws Exception {
        return String.valueOf((Object) read(result, "$.topItems")) + read(result, "$.full.attention");
    }

    private void refresh(Owner owner) {
        SummaryStatus status = summaries.due().stream()
                .filter(s -> s.establishmentId().value().equals(owner.establishmentId()))
                .findFirst().orElseThrow();
        summaries.refresh(status);
    }

    private LocalDate summarizedUntil(Owner owner) {
        List<LocalDate> until = jdbc.queryForList(
                "SELECT summarized_until FROM report_summary_state WHERE establishment_id = ?", LocalDate.class,
                owner.establishmentId());
        return until.isEmpty() ? null : until.getFirst();
    }

    @Test
    void theNumbersAreTheSameWithTheSummary() throws Exception {
        String sector = "&sectorId=" + varanda.sector();
        MvcTestResult before = data.get(sakura, overview(""));
        MvcTestResult beforeSector = data.get(sakura, overview(sector));
        assertThat(before).hasStatusOk();
        assertThat(json(before, "$.topItems[0].name")).isEqualTo("Guioza");
        assertThat((List<?>) read(before, "$.full.attention")).isNotEmpty();

        refresh(sakura);
        assertThat(summarizedUntil(sakura)).isEqualTo(today);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM report_product_day WHERE establishment_id = ?",
                Integer.class, sakura.establishmentId())).isPositive();
        // Já em dia: não volta na lista até o dia virar.
        assertThat(summaries.due()).noneMatch(s -> s.establishmentId().value().equals(sakura.establishmentId()));

        assertThat(blocks(data.get(sakura, overview("")))).isEqualTo(blocks(before));
        assertThat(blocks(data.get(sakura, overview(sector)))).isEqualTo(blocks(beforeSector));
    }

    @Test
    void movingATableToAnotherSectorDropsTheSummary() throws Exception {
        refresh(sakura);
        assertThat(summarizedUntil(sakura)).isEqualTo(today);

        MvcTestResult moved = mvc.patch().uri("/api/v1/tables/" + salao.table())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + sakura.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sectorId\":\"%s\"}".formatted(varanda.sector())).exchange();
        assertThat(moved).hasStatusOk();
        assertThat(summarizedUntil(sakura)).isNull();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM report_product_day WHERE establishment_id = ?",
                Integer.class, sakura.establishmentId())).isZero();

        // Sem o resumo, as consultas diretas já contam o passado da mesa no setor novo; e o resumo refeito também.
        String sector = "&sectorId=" + varanda.sector();
        MvcTestResult live = data.get(sakura, overview(sector));
        refresh(sakura);
        assertThat(blocks(data.get(sakura, overview(sector)))).isEqualTo(blocks(live));
    }

    /** Acesso entre restaurantes: o resumo de um restaurante nunca entra nos números de outro. */
    @Test
    void anotherRestaurantNeverSeesTheSummary() throws Exception {
        Owner tanuki = data.newRestaurant();
        data.onPlan(tanuki, "business");
        refresh(sakura);

        MvcTestResult theirs = data.get(tanuki, overview(""));
        assertThat(theirs).hasStatusOk();
        assertThat((List<?>) read(theirs, "$.topItems")).isEmpty();
        // Sem movimento, nem entra na tarefa.
        assertThat(summaries.due()).noneMatch(s -> s.establishmentId().value().equals(tanuki.establishmentId()));
    }
}
