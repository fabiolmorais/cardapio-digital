package br.com.cardapio.adapter.in.web.report.export;

import br.com.cardapio.domain.report.model.ReportPeriod;
import br.com.cardapio.domain.shared.model.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * As tabelas de uma tela dos relatórios, antes de virar arquivo (RF29): seções com título, cabeçalho e
 * linhas de células com tipo. O mesmo conteúdo vira CSV, Excel ou PDF ({@link CsvFormat},
 * {@link XlsxFormat}, {@link PdfFormat}): cada formato decide como mostrar número, dinheiro e data.
 *
 * @param title  o nome da tela ("Vendas")
 * @param period os dias do relatório, no fuso do restaurante
 */
public record Sheet(String title, ReportPeriod period, List<Section> sections) {

    /** O tipo da célula: o que o formato precisa saber para escrever certo. */
    public enum Kind { TEXT, INTEGER, DECIMAL, MONEY, DATE, EMPTY }

    /**
     * Uma célula. {@code places}: as casas decimais (DECIMAL e MONEY).
     */
    public record Cell(Kind kind, String text, BigDecimal number, LocalDate date, int places) {
    }

    /** Uma tabela: o título, o cabeçalho e as linhas. */
    public record Section(String title, List<String> header, List<List<Cell>> rows) {
    }

    public Sheet {
        Objects.requireNonNull(title, "título é obrigatório");
        Objects.requireNonNull(period, "período é obrigatório");
    }

    public Sheet(String title, ReportPeriod period) {
        this(title, period, new ArrayList<>());
    }

    private static final Cell EMPTY = new Cell(Kind.EMPTY, "", null, null, 0);

    public static Cell empty() {
        return EMPTY;
    }

    /** Um texto vindo do usuário ou do sistema. */
    public static Cell text(String value) {
        return new Cell(Kind.TEXT, value == null ? "" : value, null, null, 0);
    }

    public static Cell number(long value) {
        return new Cell(Kind.INTEGER, null, BigDecimal.valueOf(value), null, 0);
    }

    /** Um número com até {@code places} casas; vazio sem valor. */
    public static Cell decimal(Double value, int places) {
        return value == null ? EMPTY : new Cell(Kind.DECIMAL, null,
                BigDecimal.valueOf(value).setScale(places, RoundingMode.HALF_UP), null, places);
    }

    public static Cell decimal(BigDecimal value, int places) {
        return value == null ? EMPTY : decimal(value.doubleValue(), places);
    }

    /** Reais com duas casas; vazio sem valor. */
    public static Cell money(Money value) {
        return value == null ? EMPTY : new Cell(Kind.MONEY, null, BigDecimal.valueOf(value.cents(), 2), null, 2);
    }

    public static Cell money(Optional<Money> value) {
        return money(value.orElse(null));
    }

    /** Minutos com uma casa; vazio sem valor. */
    public static Cell minutes(Optional<Duration> value) {
        return value.map(Sheet::minutes).orElse(EMPTY);
    }

    public static Cell minutes(Duration value) {
        return decimal(value.toSeconds() / 60.0, 1);
    }

    public static Cell day(LocalDate value) {
        return value == null ? EMPTY : new Cell(Kind.DATE, null, null, value, 0);
    }

    /** Uma fração (0 a 1) em porcentagem, com uma casa. */
    public static Cell percent(double share) {
        return decimal(share * 100, 1);
    }

    /** Começa uma tabela: o título e o cabeçalho. */
    public Sheet section(String sectionTitle, String... header) {
        sections.add(new Section(sectionTitle, List.of(header), new ArrayList<>()));
        return this;
    }

    public Sheet row(Cell... cells) {
        sections.getLast().rows().add(List.of(cells));
        return this;
    }
}
