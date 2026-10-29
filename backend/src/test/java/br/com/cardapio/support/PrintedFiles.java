package br.com.cardapio.support;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/** Lê de volta o que o sistema imprime: o texto escondido num QR Code em PNG e o texto de um PDF. */
public final class PrintedFiles {

    private PrintedFiles() {
    }

    /** O endereço que um celular abriria ao escanear este PNG. */
    public static String readQr(byte[] png) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        RGBLuminanceSource source = new RGBLuminanceSource(image.getWidth(), image.getHeight(), pixels);
        try {
            return new QRCodeReader().decode(new BinaryBitmap(new HybridBinarizer(source))).getText();
        } catch (NotFoundException | ChecksumException | FormatException e) {
            throw new AssertionError("não há um QR Code legível na imagem", e);
        }
    }

    public static int pdfPages(byte[] pdf) throws IOException {
        try (PdfReader reader = new PdfReader(pdf)) {
            return reader.getNumberOfPages();
        }
    }

    /** O texto de uma página do PDF (a primeira é a 1). */
    public static String pdfText(byte[] pdf, int page) throws IOException {
        try (PdfReader reader = new PdfReader(pdf)) {
            return new PdfTextExtractor(reader).getTextFromPage(page);
        }
    }
}
