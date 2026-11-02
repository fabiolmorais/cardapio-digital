package br.com.cardapio.domain.brand.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;

import java.util.Set;

/**
 * O que o navegador diz sobre a logo antes de enviar, como o PhotoFile das fotos, com um limite
 * menor: uma logo não precisa de 8 MB. SVG fica de fora: é um formato de texto que pode carregar
 * código, e conferir isso direito é muito mais difícil que ler um PNG.
 */
public record LogoFile(String contentType, long sizeBytes) {

    public static final Set<String> TYPES = Set.of("image/png", "image/jpeg");
    public static final long MAX_BYTES = 2L * 1024 * 1024;

    public LogoFile {
        if (contentType == null || !TYPES.contains(contentType)) {
            throw new InvalidValueException("Envie a logo em PNG ou JPG");
        }
        if (sizeBytes <= 0 || sizeBytes > MAX_BYTES) {
            throw new InvalidValueException("A logo precisa ter até 2 MB");
        }
    }
}
