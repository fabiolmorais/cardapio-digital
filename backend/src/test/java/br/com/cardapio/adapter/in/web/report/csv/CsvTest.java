package br.com.cardapio.adapter.in.web.report.csv;

import br.com.cardapio.domain.shared.model.Money;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import static br.com.cardapio.adapter.in.web.report.csv.Csv.day;
import static br.com.cardapio.adapter.in.web.report.csv.Csv.decimal;
import static br.com.cardapio.adapter.in.web.report.csv.Csv.minutes;
import static br.com.cardapio.adapter.in.web.report.csv.Csv.money;
import static br.com.cardapio.adapter.in.web.report.csv.Csv.number;
import static br.com.cardapio.adapter.in.web.report.csv.Csv.percent;
import static br.com.cardapio.adapter.in.web.report.csv.Csv.text;
import static org.assertj.core.api.Assertions.assertThat;

class CsvTest {

    @Test
    void sectionsHaveTitleHeaderAndRowsSeparatedByABlankLine() {
        Csv csv = new Csv().section("Resumo", "Indicador", "Valor").row(text("Comandas"), number(12))
                .section("Outro", "A").row(text("b"));

        assertThat(csv.toString()).isEqualTo("Resumo\r\nIndicador;Valor\r\nComandas;12\r\n\r\nOutro\r\nA\r\nb\r\n");
    }

    @Test
    void numbersUseTheBrazilianFormat() {
        Csv csv = new Csv().section("X", "a").row(money(Money.ofCents(123450)), money(Money.ofCents(5)),
                decimal(3.456, 1), minutes(Duration.ofSeconds(90)), minutes(Optional.empty()), percent(0.685),
                day(LocalDate.of(2026, 11, 3)));

        assertThat(csv.toString()).endsWith("1234,50;0,05;3,5;1,5;;68,5;03/11/2026\r\n");
    }

    @Test
    void textWithSeparatorsQuotesOrLineBreaksIsQuoted() {
        Csv csv = new Csv().section("X", "a").row(text("Sushi; especial"), text("O \"chef\""), text("duas\nlinhas"));

        assertThat(csv.toString()).endsWith("\"Sushi; especial\";\"O \"\"chef\"\"\";\"duas\nlinhas\"\r\n");
    }

    @Test
    void textThatLooksLikeAFormulaIsNeutralized() {
        Csv csv = new Csv().section("X", "a").row(text("=HYPERLINK(\"x\")"), text("+55"), text("-1"), text("@SUM(A1)"),
                text("Normal"));

        assertThat(csv.toString()).endsWith("\"'=HYPERLINK(\"\"x\"\")\";'+55;'-1;'@SUM(A1);Normal\r\n");
    }

    @Test
    void theFileStartsWithTheUtf8Bom() {
        byte[] bytes = new Csv().section("Café", "a").bytes();

        assertThat(Arrays.copyOf(bytes, 3)).containsExactly(0xEF, 0xBB, 0xBF);
        assertThat(new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8)).startsWith("Café\r\n");
    }
}
