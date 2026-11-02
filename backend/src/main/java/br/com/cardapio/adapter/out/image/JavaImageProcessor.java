package br.com.cardapio.adapter.out.image;

import br.com.cardapio.application.port.out.media.ImageProcessorPort;
import br.com.cardapio.application.port.out.media.ProcessedImage;
import br.com.cardapio.application.port.out.media.ProcessedLogo;
import br.com.cardapio.domain.brand.model.LogoVariant;
import br.com.cardapio.domain.shared.exception.InvalidImageException;
import br.com.cardapio.domain.menu.model.ImageVariant;
import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Gera os tamanhos das fotos (JPEG) e da logo (PNG) com o ImageIO do próprio Java (nenhuma biblioteca a mais).
 *
 * Três cuidados de segurança e privacidade:
 * - o tipo real vem dos bytes (o leitor que o ImageIO acha para o arquivo), não do nome nem do
 *   Content-Type que o navegador mandou;
 * - as medidas são lidas ANTES de abrir a foto: um PNG pequeno pode dizer que tem 50.000 × 50.000
 *   pixels e, aberto, ocupar gigabytes de memória (a "bomba de descompressão");
 * - os tamanhos são gravados do zero, sem os metadados do original: o EXIF do celular pode
 *   ter a localização GPS de onde a foto foi tirada (LGPD).
 */
@Component
class JavaImageProcessor implements ImageProcessorPort {

    private static final Set<String> FORMATS = Set.of("jpeg", "png");
    /** 50 megapixels: mais que qualquer câmera de celular comum, longe do que derruba o servidor. */
    private static final long MAX_PIXELS = 50_000_000L;
    private static final int MIN_SIDE = 200;
    /** A logo pode ser menor que uma foto, mas não minúscula: 128 é o tamanho pequeno que geramos. */
    private static final int LOGO_MIN_SIDE = 128;
    private static final float JPEG_QUALITY = 0.82f;

    @Override
    public ProcessedImage process(byte[] original) {
        BufferedImage image = orient(read(original, MIN_SIDE), ExifOrientation.of(original));
        Map<ImageVariant, byte[]> variants = new EnumMap<>(ImageVariant.class);
        BufferedImage large = null;
        for (ImageVariant variant : ImageVariant.values()) {
            BufferedImage resized = resize(image, variant);
            if (variant == ImageVariant.LARGE) {
                large = resized;
            }
            variants.put(variant, jpeg(resized));
        }
        return new ProcessedImage(variants, large.getWidth(), large.getHeight());
    }

    /**
     * A logo cabe inteira num quadrado transparente, centralizada (sem cortar nada, ao contrário da
     * miniatura das fotos). Pode ser ampliada: uma logo de 200 px precisa ocupar o tamanho de 512.
     */
    @Override
    public ProcessedLogo processLogo(byte[] original) {
        BufferedImage image = orient(read(original, LOGO_MIN_SIDE), ExifOrientation.of(original));
        Map<LogoVariant, byte[]> variants = new EnumMap<>(LogoVariant.class);
        for (LogoVariant variant : LogoVariant.values()) {
            variants.put(variant, png(fitInSquare(image, variant.side())));
        }
        return new ProcessedLogo(variants);
    }

    private static BufferedImage fitInSquare(BufferedImage image, int side) {
        double scale = (double) side / Math.max(image.getWidth(), image.getHeight());
        int w = Math.max(1, (int) Math.round(image.getWidth() * scale));
        int h = Math.max(1, (int) Math.round(image.getHeight() * scale));
        BufferedImage out = new BufferedImage(side, side, BufferedImage.TYPE_INT_ARGB); // começa transparente
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(image, (side - w) / 2, (side - h) / 2, w, h, null);
        g.dispose();
        return out;
    }

    private static byte[] png(BufferedImage image) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", bytes);
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao gerar o PNG", e);
        }
        return bytes.toByteArray();
    }

    private static BufferedImage read(byte[] bytes, int minSide) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new InvalidImageException("O arquivo não é uma foto JPG ou PNG");
            }
            ImageReader reader = readers.next();
            try {
                if (!FORMATS.contains(reader.getFormatName().toLowerCase())) {
                    throw new InvalidImageException("Envie uma foto JPG ou PNG");
                }
                reader.setInput(input, true, true);
                long width = reader.getWidth(0);
                long height = reader.getHeight(0);
                if (width * height > MAX_PIXELS) {
                    throw new InvalidImageException("A foto é grande demais. Use uma de até 50 megapixels.");
                }
                if (width < minSide || height < minSide) {
                    throw new InvalidImageException(
                            "A imagem é pequena demais. Use uma de pelo menos " + minSide + " × " + minSide + " pixels.");
                }
                return reader.read(0);
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new InvalidImageException("Não foi possível ler a foto. Ela pode estar corrompida.");
        }
    }

    /** Gira ou espelha conforme o EXIF (1 = como está; 6 = girar 90° à direita, o caso comum do celular em pé). */
    private static BufferedImage orient(BufferedImage image, int orientation) {
        if (orientation == 1) {
            return image;
        }
        int w = image.getWidth();
        int h = image.getHeight();
        boolean swap = orientation >= 5;
        AffineTransform t = new AffineTransform();
        switch (orientation) {
            case 2 -> { t.translate(w, 0); t.scale(-1, 1); }
            case 3 -> { t.translate(w, h); t.rotate(Math.PI); }
            case 4 -> { t.translate(0, h); t.scale(1, -1); }
            case 5 -> { t.rotate(Math.PI / 2); t.scale(1, -1); }
            case 6 -> { t.translate(h, 0); t.rotate(Math.PI / 2); }
            case 7 -> { t.scale(-1, 1); t.translate(-h, 0); t.translate(0, w); t.rotate(3 * Math.PI / 2); }
            case 8 -> { t.translate(0, w); t.rotate(3 * Math.PI / 2); }
            default -> { return image; }
        }
        BufferedImage out = new BufferedImage(swap ? h : w, swap ? w : h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(image, t, null);
        g.dispose();
        return out;
    }

    /** Reduz (nunca aumenta) até o maior lado caber; o quadrado recorta o centro antes. */
    private static BufferedImage resize(BufferedImage image, ImageVariant variant) {
        int sx = 0;
        int sy = 0;
        int sw = image.getWidth();
        int sh = image.getHeight();
        if (variant.square()) {
            int side = Math.min(sw, sh);
            sx = (sw - side) / 2;
            sy = (sh - side) / 2;
            sw = side;
            sh = side;
        }
        double scale = Math.min(1.0, (double) variant.maxSide() / Math.max(sw, sh));
        int tw = Math.max(1, (int) Math.round(sw * scale));
        int th = Math.max(1, (int) Math.round(sh * scale));

        // Fundo branco: o PNG transparente vira JPEG sem as partes transparentes ficarem pretas.
        BufferedImage out = new BufferedImage(tw, th, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, tw, th);
        g.drawImage(image, 0, 0, tw, th, sx, sy, sx + sw, sy + sh, null);
        g.dispose();
        return out;
    }

    private static byte[] jpeg(BufferedImage image) {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (MemoryCacheImageOutputStream output = new MemoryCacheImageOutputStream(bytes)) {
            ImageWriteParam params = writer.getDefaultWriteParam();
            params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            params.setCompressionQuality(JPEG_QUALITY);
            writer.setOutput(output);
            writer.write(null, new IIOImage(image, null, null), params);
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao gerar o JPEG", e);
        } finally {
            writer.dispose();
        }
        return bytes.toByteArray();
    }
}
