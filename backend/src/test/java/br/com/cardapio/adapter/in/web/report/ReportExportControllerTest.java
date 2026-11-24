package br.com.cardapio.adapter.in.web.report;

import br.com.cardapio.adapter.in.web.report.ReportTestData.Owner;
import br.com.cardapio.adapter.in.web.report.ReportTestData.Place;
import br.com.cardapio.application.port.out.security.AccessTokenIssuer;
import br.com.cardapio.domain.account.model.Role;
import br.com.cardapio.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

import static br.com.cardapio.adapter.in.web.report.ReportTestData.SAO_PAULO;
import static br.com.cardapio.adapter.in.web.report.ReportTestData.at;
import static br.com.cardapio.adapter.in.web.report.ReportTestData.item;
import static org.assertj.core.api.Assertions.assertThat;

/** A exportação dos relatórios em CSV (R12): só o Business, só o dono e o gerente, só o próprio restaurante. */
@IntegrationTest
class ReportExportControllerTest {

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

    /** Duas comandas fechadas no último dia: R$ 50 (2 temakis, Pix) e R$ 12 (1 "=Refri; lata", cartão). */
    @BeforeEach
    void sakuraWithTwoTabs() throws Exception {
        data = new ReportTestData(mvc, jdbc, tokenIssuer);
        sakura = data.newRestaurant();
        salao = data.place(sakura, "Salão");
        UUID temaki = data.product(sakura, "Temaki", 2500);
        UUID refri = data.product(sakura, "=Refri; lata", 1200);
        data.closedTab(sakura, salao, at(day, 20, 0), at(day, 21, 0), 5000, "PIX", 0,
                item(temaki, "Temaki", 2500, 2));
        data.closedTab(sakura, salao, at(day, 12, 0), at(day, 13, 0), 1200, "CARD", 0,
                item(refri, "=Refri; lata", 1200, 1));
    }

    private String export(String screen) {
        return "/api/v1/reports/%s/export?from=%s&to=%s".formatted(screen, first, day);
    }

    private static String body(MvcTestResult result) {
        byte[] bytes = result.getResponse().getContentAsByteArray();
        return new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
    }

    @Test
    void onlyTheBusinessExports() {
        assertThat(data.get(sakura, export("sales"))).hasStatus(HttpStatus.FORBIDDEN);
        data.onPlan(sakura, "business");
        assertThat(data.get(sakura, export("sales"))).hasStatusOk();
    }

    @Test
    void theTeamCannotExport() {
        data.onPlan(sakura, "business");
        assertThat(data.get(data.token(sakura, Role.STAFF), export("sales"))).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(data.get(data.token(sakura, Role.KITCHEN), export("menu"))).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(data.get(data.token(sakura, Role.MANAGER), export("menu"))).hasStatusOk();
    }

    @ParameterizedTest
    @ValueSource(strings = {"overview", "sales", "menu", "kitchen", "team", "tables", "phone-and-rodizio"})
    void everyScreenExportsACsvFile(String screen) {
        data.onPlan(sakura, "business");

        MvcTestResult result = data.get(sakura, export(screen));

        assertThat(result).hasStatusOk();
        assertThat(result.getResponse().getContentType()).isEqualTo("text/csv;charset=UTF-8");
        assertThat(result.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION))
                .startsWith("attachment; filename=\"ohchefe-").endsWith("-%s-a-%s.csv\"".formatted(first, day));
        assertThat(result.getResponse().getContentAsByteArray()).startsWith(0xEF, 0xBB, 0xBF);
        assertThat(body(result)).startsWith(screen.equals("phone-and-rodizio") ? "Quem lança os pedidos\r\n"
                : "Resumo\r\n");
    }

    @ParameterizedTest
    @ValueSource(strings = {"overview", "sales", "menu", "kitchen", "team", "tables", "phone-and-rodizio"})
    void everyScreenExportsAnExcelAndAPdf(String screen) {
        data.onPlan(sakura, "business");

        MvcTestResult xlsx = data.get(sakura, export(screen) + "&format=XLSX");
        MvcTestResult pdf = data.get(sakura, export(screen) + "&format=PDF");

        assertThat(xlsx).hasStatusOk();
        assertThat(xlsx.getResponse().getContentType())
                .isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertThat(xlsx.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION)).endsWith(".xlsx\"");
        assertThat(xlsx.getResponse().getContentAsByteArray()).startsWith('P', 'K');
        assertThat(pdf).hasStatusOk();
        assertThat(pdf.getResponse().getContentType()).isEqualTo("application/pdf");
        assertThat(pdf.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION)).endsWith(".pdf\"");
        assertThat(new String(pdf.getResponse().getContentAsByteArray(), 0, 5, StandardCharsets.US_ASCII))
                .isEqualTo("%PDF-");
    }

    @Test
    void outsideTheBusinessNoFormatExports() {
        assertThat(data.get(sakura, export("sales") + "&format=XLSX")).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(data.get(sakura, export("sales") + "&format=PDF")).hasStatus(HttpStatus.FORBIDDEN);
        data.onPlan(sakura, "business");
        assertThat(data.get(sakura, export("sales") + "&format=DOC")).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void theSalesSheet() {
        data.onPlan(sakura, "business");

        String csv = body(data.get(sakura, export("sales")));

        assertThat(csv).contains("Faturamento (R$);62,00;0,00\r\n", "Comandas;2;0\r\n", "Tíquete médio (R$);31,00;\r\n");
        assertThat(csv).contains("%s;%s;62,00;2\r\n".formatted(day(day), day(day)));
    }

    @Test
    void theMenuSheetHasEveryProductAndNoFormula() {
        data.onPlan(sakura, "business");

        String csv = body(data.get(sakura, export("menu")));

        assertThat(csv).contains("Produto;Categoria;Quantidade;Faturamento (R$);Curva ABC");
        assertThat(csv).contains("Temaki;", "\"'=Refri; lata\";").doesNotContain(";=Refri");
    }

    @Test
    void anotherRestaurantNeverShowsUp() throws Exception {
        data.onPlan(sakura, "business");
        Owner other = data.newRestaurant();
        data.onPlan(other, "business");
        Place theirs = data.place(other, "Varanda");
        UUID ramen = data.product(other, "Lamen secreto", 4000);
        data.closedTab(other, theirs, at(day, 20, 0), at(day, 21, 0), 4000, "PIX", 0,
                item(ramen, "Lamen secreto", 4000, 1));

        assertThat(body(data.get(sakura, export("menu")))).doesNotContain("Lamen secreto");
        assertThat(body(data.get(other, export("menu")))).contains("Lamen secreto").doesNotContain("Temaki");
        assertThat(data.get(sakura, export("sales") + "&sectorId=" + theirs.sector())).hasStatus(HttpStatus.NOT_FOUND);
    }

    private static String day(LocalDate date) {
        return "%02d/%02d/%d".formatted(date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }
}
