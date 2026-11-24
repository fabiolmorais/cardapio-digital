package br.com.cardapio.adapter.in.web.report.csv;

import br.com.cardapio.domain.shared.model.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Um CSV para abrir direto no Excel em português: ponto e vírgula entre as colunas, vírgula decimal,
 * datas dd/mm/aaaa, quebra de linha CRLF e UTF-8 com BOM (sem o BOM, o Excel erra os acentos).
 *
 * <p>Cada tabela é uma seção: o título numa linha, o cabeçalho na seguinte, as linhas e uma linha em
 * branco. Texto que começa com {@code = + - @}, tabulação ou retorno ganha um apóstrofo na frente, para
 * a planilha não executar um nome de produto como fórmula (injeção de CSV, OWASP).
 */
public final class Csv {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final byte[] BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private final StringBuilder out = new StringBuilder();

    /** Uma célula já pronta (texto ou número formatado). */
    public record Cell(String value, boolean text) {
    }

    /** Um texto vindo do usuário ou do sistema: escapado e protegido contra fórmula. */
    public static Cell text(String value) {
        return new Cell(value == null ? "" : value, true);
    }

    public static Cell number(long value) {
        return new Cell(Long.toString(value), false);
    }

    /** Um número com até {@code places} casas, com vírgula decimal; vazio sem valor. */
    public static Cell decimal(Double value, int places) {
        if (value == null) return new Cell("", false);
        return new Cell(BigDecimal.valueOf(value).setScale(places, RoundingMode.HALF_UP).toPlainString()
                .replace('.', ','), false);
    }

    public static Cell decimal(BigDecimal value, int places) {
        return value == null ? new Cell("", false) : decimal(value.doubleValue(), places);
    }

    /** Reais com duas casas e vírgula ("1234,50"), sem o "R$", para a planilha somar. */
    public static Cell money(Money value) {
        if (value == null) return new Cell("", false);
        return new Cell(BigDecimal.valueOf(value.cents(), 2).toPlainString().replace('.', ','), false);
    }

    public static Cell money(Optional<Money> value) {
        return money(value.orElse(null));
    }

    /** Minutos com uma casa ("12,5"); vazio sem valor. */
    public static Cell minutes(Optional<Duration> value) {
        return value.map(Csv::minutes).orElse(new Cell("", false));
    }

    public static Cell minutes(Duration value) {
        return decimal(value.toSeconds() / 60.0, 1);
    }

    public static Cell day(LocalDate value) {
        return new Cell(value == null ? "" : DAY.format(value), false);
    }

    /** Uma fração (0 a 1) em porcentagem com uma casa ("68,5"). */
    public static Cell percent(double share) {
        return decimal(share * 100, 1);
    }

    /** Começa uma tabela: o título e o cabeçalho. */
    public Csv section(String title, String... header) {
        if (!out.isEmpty()) out.append("\r\n");
        line(List.of(text(title)));
        line(List.of(header).stream().map(Csv::text).toList());
        return this;
    }

    public Csv row(Cell... cells) {
        line(List.of(cells));
        return this;
    }

    private void line(List<Cell> cells) {
        List<String> values = new ArrayList<>();
        for (Cell cell : cells) values.add(cell.text() ? escape(cell.value()) : cell.value());
        out.append(String.join(";", values)).append("\r\n");
    }

    private static String escape(String value) {
        String safe = !value.isEmpty() && "=+-@\t\r".indexOf(value.charAt(0)) >= 0 ? "'" + value : value;
        if (safe.contains(";") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r")) {
            return "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }

    /** O arquivo, com o BOM na frente. */
    public byte[] bytes() {
        byte[] body = out.toString().getBytes(StandardCharsets.UTF_8);
        byte[] all = new byte[BOM.length + body.length];
        System.arraycopy(BOM, 0, all, 0, BOM.length);
        System.arraycopy(body, 0, all, BOM.length, body.length);
        return all;
    }

    @Override
    public String toString() {
        return out.toString();
    }
}
