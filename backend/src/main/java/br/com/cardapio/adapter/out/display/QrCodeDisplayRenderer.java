package br.com.cardapio.adapter.out.display;

import br.com.cardapio.application.port.out.floor.DisplayContent;
import br.com.cardapio.application.port.out.floor.TableDisplayRenderer;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.PageSize;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.BaseFont;
import org.openpdf.text.pdf.PdfContentByte;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * QR Code com ZXing (só a matriz de quadradinhos) e display com OpenPDF. O desenho é feito com
 * retângulos e com as fontes que vêm dentro do OpenPDF: nada depende das fontes instaladas no
 * servidor (a imagem Alpine do Fly.io não tem nenhuma).
 */
@Component
class QrCodeDisplayRenderer implements TableDisplayRenderer {

    /** Correção de erro M: o QR ainda lê com ~15% riscado ou sujo. Margem de 4 módulos, como manda o padrão. */
    private static final Map<EncodeHintType, Object> HINTS = Map.of(
            EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN, 4,
            EncodeHintType.CHARACTER_SET, "UTF-8");
    /** Pixels por módulo no PNG: um QR de ~37 módulos sai com ~600 px, bom para gráfica. */
    private static final int PNG_SCALE = 16;

    private static final Color BRAND = new Color(0xB8, 0x36, 0x1A);
    private static final Color INK = new Color(0x16, 0x13, 0x0F);
    private static final Color MUTED = new Color(0x5C, 0x55, 0x4E);

    BitMatrix encode(String url) {
        try {
            // Tamanho 0: o ZXing devolve a menor matriz possível (1 ponto por módulo); a escala é nossa.
            return new QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, 0, 0, HINTS);
        } catch (WriterException e) {
            throw new IllegalStateException("não foi possível gerar o QR Code", e);
        }
    }

    @Override
    public byte[] qrPng(String url) {
        BitMatrix matrix = encode(url);
        int size = matrix.getWidth() * PNG_SCALE;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, size, size);
            g.setColor(Color.BLACK);
            for (int y = 0; y < matrix.getHeight(); y++) {
                for (int x = 0; x < matrix.getWidth(); x++) {
                    if (matrix.get(x, y)) {
                        g.fillRect(x * PNG_SCALE, y * PNG_SCALE, PNG_SCALE, PNG_SCALE);
                    }
                }
            }
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    /** Um caminho (path) por linha, juntando os módulos pretos vizinhos: o arquivo fica pequeno. */
    @Override
    public byte[] qrSvg(String url) {
        BitMatrix matrix = encode(url);
        int w = matrix.getWidth();
        int h = matrix.getHeight();
        StringBuilder path = new StringBuilder();
        for (int y = 0; y < h; y++) {
            int x = 0;
            while (x < w) {
                if (!matrix.get(x, y)) {
                    x++;
                    continue;
                }
                int start = x;
                while (x < w && matrix.get(x, y)) {
                    x++;
                }
                path.append('M').append(start).append(' ').append(y)
                        .append('h').append(x - start).append("v1h-").append(x - start).append('z');
            }
        }
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 %d %d" shape-rendering="crispEdges">\
                <rect width="%d" height="%d" fill="#ffffff"/><path d="%s" fill="#000000"/></svg>
                """.formatted(w, h, w, h, path);
        return svg.getBytes(StandardCharsets.UTF_8);
    }

    /** Uma página A6 (10,5 × 14,8 cm, o tamanho de um display de mesa) para cada display. */
    @Override
    public byte[] displaysPdf(List<DisplayContent> displays) {
        if (displays.isEmpty()) {
            throw new IllegalArgumentException("nenhum display para gerar");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A6, 0, 0, 0, 0);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();
            BaseFont bold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            BaseFont regular = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            for (DisplayContent display : displays) {
                document.newPage();
                drawDisplay(writer.getDirectContent(), document.getPageSize(), display, bold, regular);
            }
            document.close();
        } catch (DocumentException | IOException e) {
            throw new IllegalStateException("não foi possível gerar o PDF", e);
        }
        return out.toByteArray();
    }

    private void drawDisplay(PdfContentByte canvas, Rectangle page, DisplayContent display,
                             BaseFont bold, BaseFont regular) {
        float centerX = page.getWidth() / 2;
        float maxTextWidth = page.getWidth() - 40;

        // Nome do restaurante no alto, em maiúsculas; encolhe se for comprido.
        String name = display.establishmentName().toUpperCase();
        text(canvas, bold, fitSize(bold, name, 11, maxTextWidth), BRAND, name, centerX, page.getHeight() - 42);

        // O QR Code, desenhado como retângulos (vetor: nítido em qualquer impressora).
        BitMatrix matrix = encode(display.url());
        float qrSize = 190;
        float module = qrSize / matrix.getWidth();
        float left = centerX - qrSize / 2;
        float top = page.getHeight() - 62;
        canvas.setColorFill(Color.BLACK);
        for (int y = 0; y < matrix.getHeight(); y++) {
            for (int x = 0; x < matrix.getWidth(); x++) {
                if (matrix.get(x, y)) {
                    canvas.rectangle(left + x * module, top - (y + 1) * module, module, module);
                }
            }
        }
        canvas.fill();

        float below = top - qrSize;
        text(canvas, regular, 10, MUTED, "Aponte a câmera para chamar", centerX, below - 14);
        text(canvas, regular, 10, MUTED, "o garçom e pedir", centerX, below - 27);

        String table = "Mesa " + display.tableLabel();
        text(canvas, bold, fitSize(bold, table, 30, maxTextWidth), INK, table, centerX, below - 66);
    }

    private static void text(PdfContentByte canvas, BaseFont font, float size, Color color, String value,
                             float x, float y) {
        canvas.beginText();
        canvas.setFontAndSize(font, size);
        canvas.setColorFill(color);
        canvas.showTextAligned(PdfContentByte.ALIGN_CENTER, value, x, y, 0);
        canvas.endText();
    }

    private static float fitSize(BaseFont font, String value, float preferred, float maxWidth) {
        float size = preferred;
        while (size > 6 && font.getWidthPoint(value, size) > maxWidth) {
            size -= 0.5f;
        }
        return size;
    }
}
