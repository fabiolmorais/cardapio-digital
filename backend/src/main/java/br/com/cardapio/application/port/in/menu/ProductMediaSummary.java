package br.com.cardapio.application.port.in.menu;

import br.com.cardapio.domain.menu.model.ImageVariant;
import br.com.cardapio.domain.menu.model.MediaType;
import br.com.cardapio.domain.menu.model.ProductMediaId;

import java.util.Map;

/** A mídia para a tela, com o endereço de cada tamanho. */
public record ProductMediaSummary(ProductMediaId id, MediaType type, int position, int width, int height,
                                  Map<ImageVariant, String> urls) {

    public ProductMediaSummary {
        urls = Map.copyOf(urls);
    }
}
