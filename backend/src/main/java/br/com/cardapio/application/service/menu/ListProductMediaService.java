package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.in.menu.ListProductMediaUseCase;
import br.com.cardapio.application.port.in.menu.ProductMediaSummary;
import br.com.cardapio.application.port.out.media.MediaStoragePort;
import br.com.cardapio.application.port.out.menu.ProductMediaPort;
import br.com.cardapio.application.port.out.menu.ProductPort;
import br.com.cardapio.application.port.out.video.VideoHostingPort;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.menu.model.ProductMedia;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class ListProductMediaService implements ListProductMediaUseCase {

    private final ProductPort products;
    private final ProductMediaPort media;
    private final MediaStoragePort storage;
    private final VideoHostingPort videos;

    public ListProductMediaService(ProductPort products, ProductMediaPort media, MediaStoragePort storage,
                                   VideoHostingPort videos) {
        this.products = products;
        this.media = media;
        this.storage = storage;
        this.videos = videos;
    }

    /**
     * A lista também pergunta ao serviço de vídeo pelos vídeos ainda em conversão (e grava o que
     * mudou): é assim que a tela do admin vê o vídeo ficar pronto mesmo sem o aviso (webhook),
     * que não chega a um back rodando no computador. Por isso a transação grava.
     */
    @Override
    @Transactional
    public ResultPage<ProductMediaSummary> list(EstablishmentId establishmentId, ProductId productId) {
        MenuLookups.activeProduct(products, establishmentId, productId);
        List<ProductMediaSummary> all = media.findActiveByProduct(establishmentId, productId).stream()
                .map(m -> {
                    ProductMedia synced = VideoSync.sync(m, videos);
                    if (synced != m) {
                        media.save(synced);
                    }
                    return synced;
                })
                .map(m -> ProductMediaMapper.toSummary(m, storage, videos))
                .toList();
        return new ResultPage<>(all, 0, ProductMedia.MAX_PER_PRODUCT, all.size());
    }
}
