package br.com.cardapio.adapter.out.image;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Lê a orientação (EXIF, tag 0x0112) de uma foto JPEG. O celular grava a foto "deitada" e anota
 * no EXIF que ela deve ser girada; o ImageIO do Java ignora essa anotação. Sem isto, a foto do
 * prato tirada com o celular em pé apareceria de lado.
 *
 * Só lê o que precisa: os marcadores do JPEG até o bloco EXIF (APP1) e, dentro dele, a primeira
 * tabela de tags. Qualquer coisa fora do esperado devolve 1 ("como está").
 */
final class ExifOrientation {

    private ExifOrientation() {
    }

    static int of(byte[] jpeg) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(jpeg);
            if (buffer.getShort() != (short) 0xFFD8) {
                return 1; // não é JPEG (o PNG não tem orientação)
            }
            while (buffer.remaining() > 4) {
                int marker = buffer.getShort() & 0xFFFF;
                int length = buffer.getShort() & 0xFFFF;
                if (marker == 0xFFE1 && isExif(buffer)) {
                    return readOrientation(buffer.slice(buffer.position() + 6, length - 8));
                }
                if (marker == 0xFFDA || (marker & 0xFF00) != 0xFF00) {
                    return 1; // começou a imagem (ou o arquivo é estranho) sem EXIF
                }
                buffer.position(buffer.position() + length - 2);
            }
        } catch (RuntimeException e) {
            // EXIF corrompido: a foto segue como está.
        }
        return 1;
    }

    private static boolean isExif(ByteBuffer buffer) {
        int p = buffer.position();
        return buffer.get(p) == 'E' && buffer.get(p + 1) == 'x' && buffer.get(p + 2) == 'i' && buffer.get(p + 3) == 'f';
    }

    /** O bloco TIFF: "II" (little-endian) ou "MM" (big-endian), o número 42 e o início da tabela. */
    private static int readOrientation(ByteBuffer tiff) {
        tiff.order(tiff.getShort(0) == 0x4949 ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
        int table = tiff.getInt(4);
        int entries = tiff.getShort(table) & 0xFFFF;
        for (int i = 0; i < entries; i++) {
            int entry = table + 2 + i * 12;
            if ((tiff.getShort(entry) & 0xFFFF) == 0x0112) {
                int value = tiff.getShort(entry + 8) & 0xFFFF;
                return value >= 1 && value <= 8 ? value : 1;
            }
        }
        return 1;
    }
}
