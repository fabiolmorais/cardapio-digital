package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.in.menu.ProductMediaSummary;
import br.com.cardapio.application.port.in.menu.VideoUpload;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.exception.MediaLimitReachedException;
import br.com.cardapio.domain.menu.exception.ProductNotFoundException;
import br.com.cardapio.domain.menu.exception.VideoLimitReachedException;
import br.com.cardapio.domain.menu.model.MediaStatus;
import br.com.cardapio.domain.menu.model.MediaType;
import br.com.cardapio.domain.menu.model.MenuCategory;
import br.com.cardapio.domain.menu.model.Product;
import br.com.cardapio.domain.menu.model.ProductMedia;
import br.com.cardapio.domain.menu.model.VideoFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** O vídeo do prato: o envio começa, o serviço de vídeo converte, e a galeria acompanha. */
class VideoServicesTest {

    private final MenuFakes menu = new MenuFakes();
    private final MediaFakes fakes = new MediaFakes();
    private final StartVideoUploadService start = new StartVideoUploadService(menu, fakes, fakes);
    private final ListProductMediaService list = new ListProductMediaService(menu, fakes, fakes, fakes);
    private final SyncVideoService sync = new SyncVideoService(fakes, fakes);
    private final RemoveProductMediaService remove = new RemoveProductMediaService(fakes, fakes);
    private final EstablishmentId sakura = EstablishmentId.newId();
    private final VideoFile mp4 = new VideoFile("video/mp4", 40_000_000);
    private Product guioza;

    @BeforeEach
    void menu() {
        MenuCategory entradas = menu.givenCategory(sakura, "Entradas");
        guioza = menu.givenProduct(entradas, "Guioza", 2990);
    }

    private String videoIdOf(VideoUpload upload) {
        return fakes.media.get(upload.mediaId()).videoId();
    }

    private List<ProductMediaSummary> gallery() {
        return list.list(sakura, guioza.id()).content();
    }

    @Test
    void startingCreatesTheVideoProcessingAtTheEndOfTheGallery() {
        fakes.save(ProductMedia.newImage(sakura, guioza.id(), 1440, 960, 0));

        VideoUpload upload = start.start(sakura, guioza.id(), mp4);

        ProductMedia created = fakes.media.get(upload.mediaId());
        assertThat(created.type()).isEqualTo(MediaType.VIDEO);
        assertThat(created.status()).isEqualTo(MediaStatus.PROCESSING);
        assertThat(created.position()).isEqualTo(1);
        assertThat(upload.endpoint()).isEqualTo("https://video.test/tus");
        assertThat(upload.headers()).containsEntry("VideoId", created.videoId());
        assertThatThrownBy(() -> start.start(EstablishmentId.newId(), guioza.id(), mp4))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void theGalleryAsksTheVideoServiceAndShowsTheVideoWhenReady() {
        String videoId = videoIdOf(start.start(sakura, guioza.id(), mp4));
        assertThat(gallery()).singleElement().satisfies(v -> {
            assertThat(v.status()).isEqualTo(MediaStatus.PROCESSING);
            assertThat(v.videoUrl()).isNull();
        });

        fakes.videoConverted(videoId, 1080, 1920, 18);

        assertThat(gallery()).singleElement().satisfies(v -> {
            assertThat(v.status()).isEqualTo(MediaStatus.READY);
            assertThat(v.videoUrl()).isEqualTo("https://video.test/" + videoId + ".mp4");
            assertThat(v.posterUrl()).isEqualTo("https://video.test/" + videoId + ".jpg");
            assertThat(v.durationSeconds()).isEqualTo(18);
            assertThat(v.urls()).isEmpty();
        });
    }

    @Test
    void theWebhookPathEndsInTheSameState() {
        String tooLong = videoIdOf(start.start(sakura, guioza.id(), mp4));
        String broken = videoIdOf(start.start(sakura, guioza.id(), mp4));
        fakes.videoConverted(tooLong, 1080, 1920, 75);
        fakes.videoFailed(broken);

        sync.sync(tooLong);
        sync.sync(broken);
        sync.sync("um-video-que-nao-existe");

        assertThat(fakes.findByVideoId(tooLong).orElseThrow().failure()).contains("75 segundos");
        assertThat(fakes.findByVideoId(broken).orElseThrow().failure()).isEqualTo(VideoSync.CONVERSION_FAILED);
        assertThat(fakes.findByVideoId(broken).orElseThrow().status()).isEqualTo(MediaStatus.FAILED);
    }

    @Test
    void threeVideosAtMostButAFailedOneDoesNotCount() {
        String first = videoIdOf(start.start(sakura, guioza.id(), mp4));
        start.start(sakura, guioza.id(), mp4);
        start.start(sakura, guioza.id(), mp4);
        assertThatThrownBy(() -> start.start(sakura, guioza.id(), mp4))
                .isInstanceOf(VideoLimitReachedException.class);

        fakes.videoFailed(first);
        sync.sync(first);

        assertThat(start.start(sakura, guioza.id(), mp4).mediaId()).isNotNull();
    }

    @Test
    void theGalleryStillHoldsEightItemsInAll() {
        for (int i = 0; i < 8; i++) {
            fakes.save(ProductMedia.newImage(sakura, guioza.id(), 1440, 960, i));
        }

        assertThatThrownBy(() -> start.start(sakura, guioza.id(), mp4))
                .isInstanceOf(MediaLimitReachedException.class);
    }

    @Test
    void removingAVideoDeletesItFromTheVideoService() {
        VideoUpload upload = start.start(sakura, guioza.id(), mp4);
        String videoId = videoIdOf(upload);

        remove.remove(sakura, guioza.id(), upload.mediaId());

        assertThat(fakes.media.get(upload.mediaId()).active()).isFalse();
        assertThat(fakes.videos).doesNotContainKey(videoId);
    }
}
