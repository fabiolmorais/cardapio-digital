package br.com.cardapio.adapter.in.web.report.export;

import br.com.cardapio.adapter.in.web.report.export.Sheet.Cell;
import br.com.cardapio.adapter.in.web.report.export.Sheet.Section;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * A planilha em CSV para abrir direto no Excel em português: ponto e vírgula entre as colunas, vírgula
 * decimal, datas dd/mm/aaaa, quebra de linha CRLF e UTF-8 com BOM (sem o BOM, o Excel erra os acentos).
 *
 * <p>Cada tabela é uma seção: o título numa linha, o cabeçalho na seguinte, as linhas e uma linha em
 * branco. Texto que começa com {@code = + - @}, tabulação ou retorno ganha um apóstrofo na frente, para
 * a planilha não executar um nome de produto como fórmula (injeção de CSV, OWASP).
 */
public final class CsvFormat {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final byte[] BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private CsvFormat() {
    }

    /** O texto, sem o BOM. */
    public static String text(Sheet sheet) {
        StringBuilder out = new StringBuilder();
        for (Section section : sheet.sections()) {
            if (!out.isEmpty()) out.append("\r\n");
            line(out, List.of(escape(section.title())));
            line(out, section.header().stream().map(CsvFormat::escape).toList());
            for (List<Cell> row : section.rows()) {
                line(out, row.stream().map(CsvFormat::value).toList());
            }
        }
        return out.toString();
    }

    /** O arquivo, com o BOM na frente. */
    public static byte[] bytes(Sheet sheet) {
        byte[] body = text(sheet).getBytes(StandardCharsets.UTF_8);
        byte[] all = new byte[BOM.length + body.length];
        System.arraycopy(BOM, 0, all, 0, BOM.length);
        System.arraycopy(body, 0, all, BOM.length, body.length);
        return all;
    }

    private static void line(StringBuilder out, List<String> values) {
        out.append(String.join(";", new ArrayList<>(values))).append("\r\n");
    }

    private static String value(Cell cell) {
        return switch (cell.kind()) {
            case TEXT -> escape(cell.text());
            case INTEGER, DECIMAL, MONEY -> cell.number().toPlainString().replace('.', ',');
            case DATE -> DAY.format(cell.date());
            case EMPTY -> "";
        };
    }

    private static String escape(String value) {
        String safe = !value.isEmpty() && "=+-@\t\r".indexOf(value.charAt(0)) >= 0 ? "'" + value : value;
        if (safe.contains(";") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r")) {
            return "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }
}
