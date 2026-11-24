package br.com.cardapio.adapter.in.web.report.export;

import br.com.cardapio.domain.shared.model.Money;
import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;

import static br.com.cardapio.adapter.in.web.report.export.CsvFormatTest.PERIOD;
import static br.com.cardapio.adapter.in.web.report.export.Sheet.day;
import static br.com.cardapio.adapter.in.web.report.export.Sheet.money;
import static br.com.cardapio.adapter.in.web.report.export.Sheet.number;
import static br.com.cardapio.adapter.in.web.report.export.Sheet.text;
import static org.assertj.core.api.Assertions.assertThat;

class PdfFormatTest {

    private static final Instant GENERATED = Instant.parse("2026-11-24T18:30:00Z");

    @Test
    void theFirstPageHasTheTitleThePeriodAndTheTables() throws IOException {
        Sheet sheet = new Sheet("Vendas", PERIOD)
                .section("Resumo", "Indicador", "Valor")
                .row(text("Faturamento (R$)"), money(Money.ofCents(12345678)))
                .row(text("Comandas"), number(1668))
                .section("Por dia", "Dia", "Faturamento (R$)")
                .row(day(LocalDate.of(2026, 11, 3)), money(Money.ofCents(98700)));

        byte[] pdf = PdfFormat.bytes(sheet, GENERATED);

        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        String page = new PdfTextExtractor(new PdfReader(pdf)).getTextFromPage(1);
        assertThat(page).contains("OhChefe · Vendas")
                .contains("Período: 01/11/2026 a 30/11/2026 · Gerado em 24/11/2026 às 15:30")
                .contains("123.456,78")
                .contains("Comandas 1.668")
                .contains("03/11/2026 987,00")
                .contains("Página 1");
    }

    @Test
    void anEmptyTableSaysSo() throws IOException {
        Sheet sheet = new Sheet("Cozinha e tempos", PERIOD).section("Por hora", "Hora", "Preparo (min)");

        String page = new PdfTextExtractor(new PdfReader(PdfFormat.bytes(sheet, GENERATED))).getTextFromPage(1);

        assertThat(page).contains("Por hora").contains("Nada no período.");
    }
}
