package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.in.menu.ProductMediaSummary;
import br.com.cardapio.application.port.out.media.MediaStoragePort;
import br.com.cardapio.application.port.out.video.VideoHostingPort;
import br.com.cardapio.domain.menu.model.ImageVariant;
import br.com.cardapio.domain.menu.model.MediaStatus;
import br.com.cardapio.domain.menu.model.ProductMedia;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** A mídia do domínio vira o resumo da tela, com os endereços públicos. */
final class ProductMediaMapper {

    private ProductMediaMapper() {
    }

    /** A foto: o endereço de cada tamanho. */
    static ProductMediaSummary toSummary(ProductMedia media, MediaStoragePort storage) {
        var urls = Arrays.stream(ImageVariant.values())
                .collect(Collectors.toMap(Function.identity(), v -> storage.publicUrl(media.fileOf(v))));
        return new ProductMediaSummary(media.id(), media.type(), media.position(), media.width(), media.height(), urls,
                media.status(), null, null, null, null);
    }

    /** Foto ou vídeo. O vídeo só ganha endereço para tocar quando fica pronto. */
    static ProductMediaSummary toSummary(ProductMedia media, MediaStoragePort storage, VideoHostingPort videos) {
        if (!media.isVideo()) {
            return toSummary(media, storage);
        }
        boolean ready = media.status() == MediaStatus.READY;
        return new ProductMediaSummary(media.id(), media.type(), media.position(), media.width(), media.height(),
                Map.of(), media.status(), ready ? videos.playbackUrl(media.videoId()) : null,
                ready ? videos.posterUrl(media.videoId()) : null, media.durationSeconds(), media.failure());
    }
}
