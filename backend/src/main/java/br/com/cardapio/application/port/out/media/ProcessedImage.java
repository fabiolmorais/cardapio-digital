package br.com.cardapio.application.port.out.media;

import br.com.cardapio.domain.menu.model.ImageVariant;

import java.util.Map;

/**
 * Os 3 tamanhos já em JPEG, e as medidas do grande.
 */
public record ProcessedImage(Map<ImageVariant, byte[]> variants, int width, int height) {

    public ProcessedImage {
        variants = Map.copyOf(variants);
    }
}
