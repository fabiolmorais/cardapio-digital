package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.in.menu.ProductMediaSummary;
import br.com.cardapio.application.port.out.media.MediaStoragePort;
import br.com.cardapio.domain.menu.model.ImageVariant;
import br.com.cardapio.domain.menu.model.ProductMedia;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

/** A mídia do domínio vira o resumo da tela, com o endereço público de cada tamanho. */
final class ProductMediaMapper {

    private ProductMediaMapper() {
    }

    static ProductMediaSummary toSummary(ProductMedia media, MediaStoragePort storage) {
        var urls = Arrays.stream(ImageVariant.values())
                .collect(Collectors.toMap(Function.identity(), v -> storage.publicUrl(media.fileOf(v))));
        return new ProductMediaSummary(media.id(), media.type(), media.position(), media.width(), media.height(), urls);
    }
}
