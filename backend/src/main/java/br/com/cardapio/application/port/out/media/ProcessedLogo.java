package br.com.cardapio.application.port.out.media;

import br.com.cardapio.domain.brand.model.LogoVariant;

import java.util.Map;

/** Os tamanhos da logo, já em PNG quadrado. */
public record ProcessedLogo(Map<LogoVariant, byte[]> variants) {

    public ProcessedLogo {
        variants = Map.copyOf(variants);
    }
}
