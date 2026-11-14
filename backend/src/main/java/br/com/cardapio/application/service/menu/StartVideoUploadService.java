package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.in.menu.StartVideoUploadUseCase;
import br.com.cardapio.application.port.in.menu.VideoUpload;
import br.com.cardapio.application.port.out.menu.ProductMediaPort;
import br.com.cardapio.application.port.out.menu.OptionGroupPort;
import br.com.cardapio.application.port.out.menu.ProductPort;
import br.com.cardapio.application.port.out.plan.SubscriptionPort;
import br.com.cardapio.application.port.out.video.VideoHostingPort;
import br.com.cardapio.application.service.plan.PlanAllowances;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.exception.MediaLimitReachedException;
import br.com.cardapio.domain.menu.exception.VideoLimitReachedException;
import br.com.cardapio.domain.menu.model.MediaStatus;
import br.com.cardapio.domain.menu.model.MediaOwner;
import br.com.cardapio.domain.menu.model.ProductMedia;
import br.com.cardapio.domain.menu.model.VideoFile;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

public class StartVideoUploadService implements StartVideoUploadUseCase {

    /** Um vídeo de 200 MB num 4G fraco leva tempo: a autorização vale duas horas. */
    static final Duration UPLOAD_VALIDITY = Duration.ofHours(2);

    private final ProductPort products;
    private final OptionGroupPort groups;
    private final ProductMediaPort media;
    private final VideoHostingPort videos;
    private final SubscriptionPort subscriptions;

    public StartVideoUploadService(ProductPort products, OptionGroupPort groups, ProductMediaPort media, VideoHostingPort videos,
                                   SubscriptionPort subscriptions) {
        this.products = products;
        this.groups = groups;
        this.media = media;
        this.videos = videos;
        this.subscriptions = subscriptions;
    }

    /**
     * O vídeo que falhou não conta no limite: ele não aparece no cardápio, e o dono precisa poder
     * enviar outro no lugar sem antes apagar o que deu errado.
     */
    @Override
    @Transactional
    public VideoUpload start(EstablishmentId establishmentId, MediaOwner owner, VideoFile file) {
        String ownerName = MediaOwners.requireActive(products, groups, establishmentId, owner);
        PlanAllowances.requireVideos(subscriptions, establishmentId);
        List<ProductMedia> gallery = media.findActiveByOwner(establishmentId, owner);
        if (gallery.size() >= ProductMedia.MAX_PER_PRODUCT) {
            throw new MediaLimitReachedException();
        }
        long videosNow = gallery.stream().filter(m -> m.isVideo() && m.status() != MediaStatus.FAILED).count();
        if (videosNow >= ProductMedia.MAX_VIDEOS_PER_PRODUCT) {
            throw new VideoLimitReachedException();
        }
        String videoId = videos.createVideo(ownerName);
        ProductMedia video = ProductMedia.newVideo(establishmentId, owner, videoId,
                media.nextPosition(establishmentId, owner));
        media.save(video);
        var ticket = videos.signUpload(videoId, UPLOAD_VALIDITY);
        return new VideoUpload(video.id(), ticket.endpoint(), ticket.headers(), ticket.expiresAt());
    }
}
