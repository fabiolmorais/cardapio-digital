package br.com.cardapio.adapter.in.web.report.export;

import br.com.cardapio.adapter.in.web.report.export.Sheet.Cell;
import br.com.cardapio.adapter.in.web.report.export.Sheet.Section;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.BaseFont;
import org.openpdf.text.pdf.ColumnText;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfPageEventHelper;
import org.openpdf.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * A planilha em PDF, em A4 (deitado quando alguma tabela tem mais de 6 colunas): o cabeçalho com a tela,
 * o período e a hora em que foi gerado, as tabelas com o cabeçalho repetido a cada página, os números à
 * direita no formato brasileiro e o número da página no rodapé. Usa a OpenPDF, que o projeto já tem.
 */
public final class PdfFormat {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");
    private static final Color ACCENT = new Color(0xCC, 0x3F, 0x1C);
    private static final Color MUTED = new Color(0x57, 0x53, 0x4E);
    private static final Color HEADER = new Color(0xF2, 0xEE, 0xE9);
    private static final Color RULE = new Color(0xD9, 0xD0, 0xC7);

    private PdfFormat() {
    }

    private static DecimalFormat decimals(int places) {
        DecimalFormat format = new DecimalFormat(places == 0 ? "#,##0" : "#,##0." + "0".repeat(places),
                DecimalFormatSymbols.getInstance(Locale.forLanguageTag("pt-BR")));
        format.setGroupingUsed(true);
        return format;
    }

    public static byte[] bytes(Sheet sheet, Instant generatedAt) {
        int widest = sheet.sections().stream().mapToInt(s -> s.header().size()).max().orElse(1);
        Rectangle page = widest > 6 ? PageSize.A4.rotate() : PageSize.A4;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            BaseFont regular = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            BaseFont bold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            Font title = new Font(bold, 18, Font.NORMAL, ACCENT);
            Font subtitle = new Font(regular, 10, Font.NORMAL, MUTED);
            Font sectionFont = new Font(bold, 12);
            Font headerFont = new Font(bold, 9);
            Font cellFont = new Font(regular, 9);

            Document document = new Document(page, 40, 40, 40, 48);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new PageNumbers(regular));
            document.addTitle("OhChefe · " + sheet.title());
            document.open();

            document.add(new Paragraph("OhChefe · " + sheet.title(), title));
            String period = sheet.period().from().equals(sheet.period().to())
                    ? DAY.format(sheet.period().from())
                    : DAY.format(sheet.period().from()) + " a " + DAY.format(sheet.period().to());
            Paragraph meta = new Paragraph("Período: " + period + " · Gerado em "
                    + WHEN.format(generatedAt.atZone(sheet.period().zone())), subtitle);
            meta.setSpacingAfter(12);
            document.add(meta);

            for (Section section : sheet.sections()) {
                // A entrelinha folgada e o espaço embaixo deixam a cedilha e o "g" do título longe da tabela.
                Paragraph heading = new Paragraph(18, section.title(), sectionFont);
                heading.setSpacingBefore(10);
                heading.setSpacingAfter(10);
                heading.setKeepTogether(true);
                document.add(heading);
                document.add(table(section, headerFont, cellFont));
            }
            document.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("PDF do relatório não gerado", e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    private static PdfPTable table(Section section, Font headerFont, Font cellFont) {
        int columns = Math.max(section.header().size(), 1);
        PdfPTable table = new PdfPTable(columns);
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        table.setSplitLate(false);
        // O espaço entre o título da seção e a tabela vai aqui: o "depois" do parágrafo não vale antes de tabela.
        table.setSpacingBefore(8);
        for (String name : section.header()) {
            PdfPCell cell = new PdfPCell(new Phrase(name, headerFont));
            cell.setBackgroundColor(HEADER);
            cell.setBorderColor(RULE);
            cell.setPadding(5);
            table.addCell(cell);
        }
        for (List<Cell> row : section.rows()) {
            for (int c = 0; c < columns; c++) {
                Cell value = c < row.size() ? row.get(c) : Sheet.empty();
                PdfPCell cell = new PdfPCell(new Phrase(shown(value), cellFont));
                cell.setBorderColor(RULE);
                cell.setPadding(4);
                boolean numeric = value.kind() == Sheet.Kind.INTEGER || value.kind() == Sheet.Kind.DECIMAL
                        || value.kind() == Sheet.Kind.MONEY;
                cell.setHorizontalAlignment(numeric ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
                table.addCell(cell);
            }
        }
        if (section.rows().isEmpty()) {
            PdfPCell none = new PdfPCell(new Phrase("Nada no período.", cellFont));
            none.setColspan(columns);
            none.setBorderColor(RULE);
            none.setPadding(4);
            table.addCell(none);
        }
        return table;
    }

    static String shown(Cell cell) {
        return switch (cell.kind()) {
            case TEXT -> cell.text();
            case INTEGER -> decimals(0).format(cell.number());
            case DECIMAL, MONEY -> decimals(cell.places()).format(cell.number());
            case DATE -> DAY.format(cell.date());
            case EMPTY -> "";
        };
    }

    /** "Página 2" no rodapé de cada página. */
    private static final class PageNumbers extends PdfPageEventHelper {

        private final BaseFont font;

        PageNumbers(BaseFont font) {
            this.font = font;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            Rectangle page = document.getPageSize();
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_RIGHT,
                    new Phrase("Página " + writer.getPageNumber(), new Font(font, 8, Font.NORMAL, MUTED)),
                    page.getRight() - 40, page.getBottom() + 24, 0);
        }
    }
}
