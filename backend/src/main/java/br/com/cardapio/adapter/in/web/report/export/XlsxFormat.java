package br.com.cardapio.adapter.in.web.report.export;

import br.com.cardapio.adapter.in.web.report.export.Sheet.Cell;
import br.com.cardapio.adapter.in.web.report.export.Sheet.Section;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * A planilha em Excel (.xlsx), escrita à mão, sem biblioteca: o formato é um zip com alguns XML (Office
 * Open XML). Uma aba por seção, com o título e o cabeçalho em negrito; números como número (a planilha
 * soma), dinheiro com duas casas e separador de milhar, datas como data.
 *
 * <p>Texto vai como texto ("inlineStr"): no .xlsx, só um elemento de fórmula vira fórmula, então um nome
 * de produto começando com "=" não é executado.
 */
public final class XlsxFormat {

    /** Os estilos de styles.xml, pela posição em cellXfs. */
    private static final int BOLD = 1;
    private static final int MONEY = 2;
    private static final int INTEGER = 3;
    private static final int ONE_PLACE = 4;
    private static final int TWO_PLACES = 5;
    private static final int DATE = 6;

    /** O dia zero das datas do Excel (com o 29/02/1900 que não existiu, como o próprio Excel conta). */
    private static final LocalDate EPOCH = LocalDate.of(1899, 12, 30);

    private XlsxFormat() {
    }

    public static byte[] bytes(Sheet sheet) {
        List<String> names = sheetNames(sheet);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            put(zip, "[Content_Types].xml", contentTypes(names.size()));
            put(zip, "_rels/.rels", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">\
                    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>\
                    </Relationships>""");
            put(zip, "xl/workbook.xml", workbook(names));
            put(zip, "xl/_rels/workbook.xml.rels", workbookRels(names.size()));
            put(zip, "xl/styles.xml", STYLES);
            for (int i = 0; i < names.size(); i++) {
                put(zip, "xl/worksheets/sheet" + (i + 1) + ".xml", worksheet(sheet.sections().get(i)));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    private static void put(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    /** O nome de cada aba: até 31 letras, sem {@code \ / ? * [ ] :}, sem repetir. */
    static List<String> sheetNames(Sheet sheet) {
        List<String> names = new ArrayList<>();
        Set<String> used = new HashSet<>();
        for (Section section : sheet.sections()) {
            String base = section.title().replaceAll("[\\\\/?*\\[\\]:]", " ").strip();
            if (base.isEmpty()) base = "Tabela";
            if (base.length() > 31) base = base.substring(0, 31).strip();
            String name = base;
            for (int n = 2; used.contains(name.toLowerCase()); n++) {
                String suffix = " (" + n + ")";
                String room = base.length() + suffix.length() > 31 ? base.substring(0, 31 - suffix.length()) : base;
                name = room.strip() + suffix;
            }
            used.add(name.toLowerCase());
            names.add(name);
        }
        return names;
    }

    private static String contentTypes(int sheets) {
        StringBuilder xml = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">\
                <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>\
                <Default Extension="xml" ContentType="application/xml"/>\
                <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>\
                <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""");
        for (int i = 1; i <= sheets; i++) {
            xml.append("<Override PartName=\"/xl/worksheets/sheet").append(i).append(".xml\" ContentType=\"")
                    .append("application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
        }
        return xml.append("</Types>").toString();
    }

    private static String workbook(List<String> names) {
        StringBuilder xml = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" \
                xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets>""");
        for (int i = 0; i < names.size(); i++) {
            xml.append("<sheet name=\"").append(escape(names.get(i))).append("\" sheetId=\"").append(i + 1)
                    .append("\" r:id=\"rId").append(i + 1).append("\"/>");
        }
        return xml.append("</sheets></workbook>").toString();
    }

    private static String workbookRels(int sheets) {
        StringBuilder xml = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""");
        for (int i = 1; i <= sheets; i++) {
            xml.append("<Relationship Id=\"rId").append(i).append("\" Type=\"http://schemas.openxmlformats.org/")
                    .append("officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet").append(i)
                    .append(".xml\"/>");
        }
        xml.append("<Relationship Id=\"rId").append(sheets + 1).append("\" Type=\"http://schemas.openxmlformats.org/")
                .append("officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>");
        return xml.append("</Relationships>").toString();
    }

    /** Fonte normal e negrito; dinheiro (#,##0.00), inteiro (#,##0), uma e duas casas e data. */
    private static final String STYLES = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">\
            <numFmts count="1"><numFmt numFmtId="164" formatCode="0.0"/></numFmts>\
            <fonts count="2"><font><sz val="11"/><name val="Calibri"/></font>\
            <font><b/><sz val="11"/><name val="Calibri"/></font></fonts>\
            <fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill></fills>\
            <borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>\
            <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>\
            <cellXfs count="7">\
            <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>\
            <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>\
            <xf numFmtId="4" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>\
            <xf numFmtId="3" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>\
            <xf numFmtId="164" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>\
            <xf numFmtId="2" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>\
            <xf numFmtId="14" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>\
            </cellXfs></styleSheet>""";

    private static String worksheet(Section section) {
        int columns = Math.max(section.header().size(), 1);
        int[] widths = new int[columns];
        widths[0] = Math.max(widths[0], section.title().length());
        for (int c = 0; c < section.header().size(); c++) widths[c] = Math.max(widths[c], section.header().get(c).length());
        for (List<Cell> row : section.rows()) {
            for (int c = 0; c < Math.min(row.size(), columns); c++) widths[c] = Math.max(widths[c], shown(row.get(c)));
        }
        StringBuilder xml = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><cols>""");
        for (int c = 0; c < columns; c++) {
            double width = Math.min(60, Math.max(10, widths[c] * 1.15 + 2));
            xml.append("<col min=\"").append(c + 1).append("\" max=\"").append(c + 1).append("\" width=\"")
                    .append(String.format(java.util.Locale.ROOT, "%.1f", width)).append("\" customWidth=\"1\"/>");
        }
        xml.append("</cols><sheetData>");
        int r = 1;
        xml.append("<row r=\"").append(r).append("\">").append(text(ref(0, r), section.title(), BOLD)).append("</row>");
        r++;
        xml.append("<row r=\"").append(r).append("\">");
        for (int c = 0; c < section.header().size(); c++) xml.append(text(ref(c, r), section.header().get(c), BOLD));
        xml.append("</row>");
        for (List<Cell> row : section.rows()) {
            r++;
            xml.append("<row r=\"").append(r).append("\">");
            for (int c = 0; c < row.size(); c++) xml.append(cell(ref(c, r), row.get(c)));
            xml.append("</row>");
        }
        return xml.append("</sheetData></worksheet>").toString();
    }

    private static int shown(Cell cell) {
        return switch (cell.kind()) {
            case TEXT -> cell.text().length();
            case INTEGER, DECIMAL, MONEY -> cell.number().toPlainString().length() + 3;
            case DATE -> 10;
            case EMPTY -> 0;
        };
    }

    private static String cell(String ref, Cell cell) {
        return switch (cell.kind()) {
            case TEXT -> text(ref, cell.text(), 0);
            case INTEGER -> number(ref, cell, INTEGER);
            case MONEY -> number(ref, cell, MONEY);
            case DECIMAL -> number(ref, cell, cell.places() >= 2 ? TWO_PLACES : ONE_PLACE);
            case DATE -> "<c r=\"" + ref + "\" s=\"" + DATE + "\"><v>"
                    + ChronoUnit.DAYS.between(EPOCH, cell.date()) + "</v></c>";
            case EMPTY -> "";
        };
    }

    private static String number(String ref, Cell cell, int style) {
        return "<c r=\"" + ref + "\" s=\"" + style + "\"><v>" + cell.number().toPlainString() + "</v></c>";
    }

    private static String text(String ref, String value, int style) {
        return "<c r=\"" + ref + "\" t=\"inlineStr\"" + (style == 0 ? "" : " s=\"" + style + "\"")
                + "><is><t xml:space=\"preserve\">" + escape(value) + "</t></is></c>";
    }

    /** "A1", "B7", "AA3". */
    static String ref(int column, int row) {
        StringBuilder letters = new StringBuilder();
        for (int c = column + 1; c > 0; c = (c - 1) / 26) letters.insert(0, (char) ('A' + (c - 1) % 26));
        return letters.toString() + row;
    }

    /** Escapa para XML e tira os caracteres de controle que o XML não aceita. */
    static String escape(String value) {
        StringBuilder out = new StringBuilder(value.length());
        value.codePoints().forEach(cp -> {
            switch (cp) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                default -> {
                    if (cp == 0x9 || cp == 0xA || cp == 0xD || (cp >= 0x20 && cp != 0xFFFE && cp != 0xFFFF)) {
                        out.appendCodePoint(cp);
                    }
                }
            }
        });
        return out.toString();
    }
}
