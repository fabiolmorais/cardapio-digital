package br.com.cardapio.adapter.in.web.report.export;

import br.com.cardapio.domain.shared.model.Money;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static br.com.cardapio.adapter.in.web.report.export.CsvFormatTest.PERIOD;
import static br.com.cardapio.adapter.in.web.report.export.Sheet.day;
import static br.com.cardapio.adapter.in.web.report.export.Sheet.decimal;
import static br.com.cardapio.adapter.in.web.report.export.Sheet.money;
import static br.com.cardapio.adapter.in.web.report.export.Sheet.number;
import static br.com.cardapio.adapter.in.web.report.export.Sheet.text;
import static org.assertj.core.api.Assertions.assertThat;

class XlsxFormatTest {

    /** Os arquivos de dentro do .xlsx (que é um zip), pelo nome. */
    private static Map<String, String> unzip(byte[] xlsx) throws IOException {
        Map<String, String> files = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(xlsx))) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                files.put(entry.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return files;
    }

    private static Sheet sheet() {
        return new Sheet("Vendas", PERIOD)
                .section("Resumo", "Indicador", "Valor", "Média", "Dia")
                .row(text("Faturamento (R$)"), money(Money.ofCents(123450)), decimal(3.25, 1),
                        day(LocalDate.of(2026, 11, 3)))
                .row(text("Comandas"), number(12))
                .section("Produtos & <categorias>", "Produto")
                .row(text("=Refri; lata"));
    }

    @Test
    void oneTabPerSectionWithTheOfficeParts() throws IOException {
        Map<String, String> files = unzip(XlsxFormat.bytes(sheet()));

        assertThat(files).containsKeys("[Content_Types].xml", "_rels/.rels", "xl/workbook.xml",
                "xl/_rels/workbook.xml.rels", "xl/styles.xml", "xl/worksheets/sheet1.xml", "xl/worksheets/sheet2.xml");
        assertThat(files.get("xl/workbook.xml"))
                .contains("<sheet name=\"Resumo\" sheetId=\"1\" r:id=\"rId1\"/>")
                .contains("<sheet name=\"Produtos &amp; &lt;categorias&gt;\" sheetId=\"2\"");
    }

    @Test
    void numbersAreNumbersAndTextIsNeverAFormula() throws IOException {
        Map<String, String> files = unzip(XlsxFormat.bytes(sheet()));
        String resumo = files.get("xl/worksheets/sheet1.xml");

        assertThat(resumo).contains("<c r=\"B3\" s=\"2\"><v>1234.50</v></c>")
                .contains("<c r=\"C3\" s=\"4\"><v>3.3</v></c>")
                .contains("<c r=\"D3\" s=\"6\"><v>46329</v></c>")
                .contains("<c r=\"B4\" s=\"3\"><v>12</v></c>")
                .contains("<c r=\"A1\" t=\"inlineStr\" s=\"1\"><is><t xml:space=\"preserve\">Resumo</t></is></c>");
        assertThat(files.get("xl/worksheets/sheet2.xml")).contains("=Refri; lata").doesNotContain("<f>");
    }

    @Test
    void tabNamesFitTheExcelRules() {
        Sheet sheet = new Sheet("X", PERIOD).section("Por hora: cozinha / salão [teste]", "a")
                .section("Por hora: cozinha / salão [teste]", "a")
                .section("Um título muito comprido que passa dos trinta e um caracteres", "a");

        assertThat(XlsxFormat.sheetNames(sheet)).containsExactly("Por hora  cozinha   salão  test",
                "Por hora  cozinha   salão (2)", "Um título muito comprido que pa");
    }

    @Test
    void columnReferencesGoPastZ() {
        assertThat(XlsxFormat.ref(0, 1)).isEqualTo("A1");
        assertThat(XlsxFormat.ref(25, 2)).isEqualTo("Z2");
        assertThat(XlsxFormat.ref(26, 3)).isEqualTo("AA3");
    }
}
