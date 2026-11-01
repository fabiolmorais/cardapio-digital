package br.com.cardapio.support;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/** Fotos geradas na hora para os testes: nenhum arquivo de imagem no repositório. */
public final class TestImages {

    private TestImages() {
    }

    /** Metade de cima vermelha e de baixo azul: dá para ver se a foto girou. */
    public static byte[] encode(String format, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, width, height / 2);
        g.setColor(Color.BLUE);
        g.fillRect(0, height / 2, width, height - height / 2);
        g.dispose();
        return write(image, format);
    }

    public static byte[] write(BufferedImage image, String format) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, format, out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static BufferedImage read(byte[] bytes) {
        try {
            return ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Põe um bloco EXIF com a orientação logo depois do início do JPEG, como o celular faz.
     * Tabela TIFF little-endian ("II") com uma tag só: 0x0112 (orientação).
     */
    public static byte[] withExifOrientation(byte[] jpeg, int orientation) {
        byte[] tiff = {
                'I', 'I', 42, 0, 8, 0, 0, 0,          // cabeçalho; a tabela começa no byte 8
                1, 0,                                   // 1 entrada
                0x12, 0x01, 3, 0, 1, 0, 0, 0,           // tag 0x0112, tipo SHORT, 1 valor
                (byte) orientation, 0, 0, 0,            // o valor
                0, 0, 0, 0                              // sem próxima tabela
        };
        byte[] exif = new byte[6 + tiff.length];
        System.arraycopy(new byte[]{'E', 'x', 'i', 'f', 0, 0}, 0, exif, 0, 6);
        System.arraycopy(tiff, 0, exif, 6, tiff.length);
        int length = exif.length + 2;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(jpeg, 0, 2);                          // FFD8: início do JPEG
        out.write(0xFF);
        out.write(0xE1);                                // APP1
        out.write(length >> 8);
        out.write(length & 0xFF);
        out.write(exif, 0, exif.length);
        out.write(jpeg, 2, jpeg.length - 2);
        return out.toByteArray();
    }
}
