package br.com.cardapio.adapter.out.image;

import br.com.cardapio.application.port.out.media.ProcessedImage;
import br.com.cardapio.domain.menu.exception.InvalidImageException;
import br.com.cardapio.domain.menu.model.ImageVariant;
import br.com.cardapio.support.TestImages;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Teste do adaptador sem Spring: só o ImageIO do Java. */
class JavaImageProcessorTest {

    private final JavaImageProcessor processor = new JavaImageProcessor();

    private static BufferedImage variant(ProcessedImage image, ImageVariant variant) {
        return TestImages.read(image.variants().get(variant));
    }

    @Test
    void makesTheThreeSizesInJpeg() {
        ProcessedImage image = processor.process(TestImages.encode("png", 3000, 2000));

        assertThat(variant(image, ImageVariant.LARGE)).extracting(BufferedImage::getWidth, BufferedImage::getHeight)
                .containsExactly(1440, 960);
        assertThat(variant(image, ImageVariant.MEDIUM)).extracting(BufferedImage::getWidth, BufferedImage::getHeight)
                .containsExactly(720, 480);
        assertThat(variant(image, ImageVariant.THUMB)).extracting(BufferedImage::getWidth, BufferedImage::getHeight)
                .containsExactly(240, 240);
        assertThat(image.width()).isEqualTo(1440);
        assertThat(image.height()).isEqualTo(960);
        // Todo tamanho sai em JPEG (começa com FF D8), mesmo vindo de um PNG.
        assertThat(image.variants().values()).allSatisfy(bytes -> assertThat(bytes).startsWith(0xFF, 0xD8));
    }

    @Test
    void neverEnlargesASmallPhoto() {
        ProcessedImage image = processor.process(TestImages.encode("jpeg", 600, 400));

        assertThat(variant(image, ImageVariant.LARGE).getWidth()).isEqualTo(600);
        assertThat(variant(image, ImageVariant.MEDIUM).getWidth()).isEqualTo(600);
    }

    @Test
    void turnsAPhoneHeldUprightTheRightWay() {
        // O celular grava 400 × 300 "deitada" e anota no EXIF: girar 90° à direita (6).
        byte[] upright = TestImages.withExifOrientation(TestImages.encode("jpeg", 400, 300), 6);

        BufferedImage large = variant(processor.process(upright), ImageVariant.LARGE);

        assertThat(large.getWidth()).isEqualTo(300);
        assertThat(large.getHeight()).isEqualTo(400);
        // O vermelho (em cima, antes de girar) foi para a direita.
        assertThat(new Color(large.getRGB(290, 200)).getRed()).isGreaterThan(200);
        assertThat(new Color(large.getRGB(10, 200)).getBlue()).isGreaterThan(200);
    }

    @Test
    void theRealTypeComesFromTheBytes() {
        byte[] text = "não sou uma foto".getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> processor.process(text))
                .isInstanceOf(InvalidImageException.class)
                .hasMessage("O arquivo não é uma foto JPG ou PNG");

        // GIF é imagem, mas não é um dos tipos aceitos.
        assertThatThrownBy(() -> processor.process(TestImages.encode("gif", 400, 400)))
                .isInstanceOf(InvalidImageException.class)
                .hasMessage("Envie uma foto JPG ou PNG");
    }

    @Test
    void refusesTinyPhotosAndDecompressionBombs() {
        assertThatThrownBy(() -> processor.process(TestImages.encode("png", 150, 150)))
                .isInstanceOf(InvalidImageException.class)
                .hasMessageContaining("pequena demais");

        // 60 megapixels em preto e branco: o PNG fica pequeno, mas aberto ocuparia muita memória.
        BufferedImage huge = new BufferedImage(10_000, 6_000, BufferedImage.TYPE_BYTE_BINARY);
        byte[] bomb = TestImages.write(huge, "png");
        assertThat(bomb.length).isLessThan(1_000_000);
        assertThatThrownBy(() -> processor.process(bomb))
                .isInstanceOf(InvalidImageException.class)
                .hasMessageContaining("grande demais");
    }

    @Test
    void exifOrientationReaderIsForgiving() {
        assertThat(ExifOrientation.of(TestImages.encode("jpeg", 300, 300))).isEqualTo(1); // sem EXIF
        assertThat(ExifOrientation.of(TestImages.encode("png", 300, 300))).isEqualTo(1);  // não é JPEG
        assertThat(ExifOrientation.of(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF})).isEqualTo(1); // cortado
        assertThat(ExifOrientation.of(TestImages.withExifOrientation(TestImages.encode("jpeg", 300, 300), 8)))
                .isEqualTo(8);
    }
}
