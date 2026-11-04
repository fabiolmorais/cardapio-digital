package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.out.video.HostedVideo;
import br.com.cardapio.application.port.out.video.VideoHostingPort;
import br.com.cardapio.domain.menu.model.MediaStatus;
import br.com.cardapio.domain.menu.model.ProductMedia;

/**
 * Traz o status de um vídeo em conversão do serviço de vídeo para a mídia. Usado pelo aviso do
 * serviço (webhook) e pela lista do admin: o serviço de vídeo de verdade não alcança um back
 * rodando no computador (localhost), e a lista pergunta ela mesma.
 */
final class VideoSync {

    static final String CONVERSION_FAILED = "O serviço de vídeo não conseguiu converter este arquivo. Envie outro.";

    private VideoSync() {
    }

    /** A mídia atualizada, ou a mesma, se nada mudou (ou se não é um vídeo em conversão). */
    static ProductMedia sync(ProductMedia media, VideoHostingPort videos) {
        if (!media.isVideo() || media.status() != MediaStatus.PROCESSING) {
            return media;
        }
        HostedVideo hosted = videos.find(media.videoId()).orElse(null);
        if (hosted == null || hosted.status() == HostedVideo.Status.PROCESSING) {
            return media;
        }
        return hosted.status() == HostedVideo.Status.READY
                ? media.converted(hosted.width(), hosted.height(), hosted.durationSeconds())
                : media.failed(CONVERSION_FAILED);
    }
}
