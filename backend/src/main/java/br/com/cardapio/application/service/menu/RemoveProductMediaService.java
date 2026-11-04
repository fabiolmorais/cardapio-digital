package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.in.menu.RemoveProductMediaUseCase;
import br.com.cardapio.application.port.out.menu.ProductMediaPort;
import br.com.cardapio.application.port.out.video.VideoHostingPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.exception.ProductMediaNotFoundException;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.menu.model.ProductMedia;
import br.com.cardapio.domain.menu.model.ProductMediaId;
import org.springframework.transaction.annotation.Transactional;

public class RemoveProductMediaService implements RemoveProductMediaUseCase {

    private final ProductMediaPort media;
    private final VideoHostingPort videos;

    public RemoveProductMediaService(ProductMediaPort media, VideoHostingPort videos) {
        this.media = media;
        this.videos = videos;
    }

    @Override
    @Transactional
    public void remove(EstablishmentId establishmentId, ProductId productId, ProductMediaId mediaId) {
        // A foto precisa ser deste produto: o id da URL não pode apontar para a foto de outro.
        ProductMedia found = media.find(establishmentId, mediaId)
                .filter(m -> m.productId().equals(productId))
                .orElseThrow(ProductMediaNotFoundException::new);
        if (found.active()) {
            media.save(found.deactivate());
            // O vídeo sai também do serviço de vídeo: guardado lá, ele custaria todo mês.
            if (found.isVideo()) {
                videos.deleteVideo(found.videoId());
            }
        }
    }
}
