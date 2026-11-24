package br.com.cardapio.adapter.in.web.report.export;

import br.com.cardapio.domain.report.model.ReportPeriod;
import br.com.cardapio.domain.shared.model.Money;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Optional;

import static br.com.cardapio.adapter.in.web.report.export.Sheet.day;
import static br.com.cardapio.adapter.in.web.report.export.Sheet.decimal;
import static br.com.cardapio.adapter.in.web.report.export.Sheet.minutes;
import static br.com.cardapio.adapter.in.web.report.export.Sheet.money;
import static br.com.cardapio.adapter.in.web.report.export.Sheet.number;
import static br.com.cardapio.adapter.in.web.report.export.Sheet.percent;
import static br.com.cardapio.adapter.in.web.report.export.Sheet.text;
import static org.assertj.core.api.Assertions.assertThat;

class CsvFormatTest {

    static final ReportPeriod PERIOD = new ReportPeriod(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
            ZoneId.of("America/Sao_Paulo"));

    private static Sheet sheet() {
        return new Sheet("Vendas", PERIOD);
    }

    @Test
    void sectionsHaveTitleHeaderAndRowsSeparatedByABlankLine() {
        Sheet sheet = sheet().section("Resumo", "Indicador", "Valor").row(text("Comandas"), number(12))
                .section("Outro", "A").row(text("b"));

        assertThat(CsvFormat.text(sheet))
                .isEqualTo("Resumo\r\nIndicador;Valor\r\nComandas;12\r\n\r\nOutro\r\nA\r\nb\r\n");
    }

    @Test
    void numbersUseTheBrazilianFormat() {
        Sheet sheet = sheet().section("X", "a").row(money(Money.ofCents(123450)), money(Money.ofCents(5)),
                decimal(3.456, 1), minutes(Duration.ofSeconds(90)), minutes(Optional.empty()), percent(0.685),
                day(LocalDate.of(2026, 11, 3)));

        assertThat(CsvFormat.text(sheet)).endsWith("1234,50;0,05;3,5;1,5;;68,5;03/11/2026\r\n");
    }

    @Test
    void textWithSeparatorsQuotesOrLineBreaksIsQuoted() {
        Sheet sheet = sheet().section("X", "a").row(text("Sushi; especial"), text("O \"chef\""), text("duas\nlinhas"));

        assertThat(CsvFormat.text(sheet)).endsWith("\"Sushi; especial\";\"O \"\"chef\"\"\";\"duas\nlinhas\"\r\n");
    }

    @Test
    void textThatLooksLikeAFormulaIsNeutralized() {
        Sheet sheet = sheet().section("X", "a").row(text("=HYPERLINK(\"x\")"), text("+55"), text("-1"),
                text("@SUM(A1)"), text("Normal"));

        assertThat(CsvFormat.text(sheet)).endsWith("\"'=HYPERLINK(\"\"x\"\")\";'+55;'-1;'@SUM(A1);Normal\r\n");
    }

    @Test
    void theFileStartsWithTheUtf8Bom() {
        byte[] bytes = CsvFormat.bytes(sheet().section("Café", "a"));

        assertThat(Arrays.copyOf(bytes, 3)).containsExactly(0xEF, 0xBB, 0xBF);
        assertThat(new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8)).startsWith("Café\r\n");
    }
}
