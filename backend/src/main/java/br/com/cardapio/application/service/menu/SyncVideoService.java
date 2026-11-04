package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.in.menu.SyncVideoUseCase;
import br.com.cardapio.application.port.out.menu.ProductMediaPort;
import br.com.cardapio.application.port.out.video.VideoHostingPort;
import br.com.cardapio.domain.menu.model.ProductMedia;
import org.springframework.transaction.annotation.Transactional;

public class SyncVideoService implements SyncVideoUseCase {

    private final ProductMediaPort media;
    private final VideoHostingPort videos;

    public SyncVideoService(ProductMediaPort media, VideoHostingPort videos) {
        this.media = media;
        this.videos = videos;
    }

    @Override
    @Transactional
    public void sync(String videoId) {
        media.findByVideoId(videoId).filter(ProductMedia::active).ifPresent(found -> {
            ProductMedia synced = VideoSync.sync(found, videos);
            if (synced != found) {
                media.save(synced);
            }
        });
    }
}
