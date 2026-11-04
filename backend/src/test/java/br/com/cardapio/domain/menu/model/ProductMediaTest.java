package br.com.cardapio.domain.menu.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductMediaTest {

    private final EstablishmentId sakura = EstablishmentId.newId();
    private final ProductId guioza = ProductId.newId();

    @Test
    void theFolderCarriesTheRestaurantAndTheProduct() {
        ProductMedia photo = ProductMedia.newImage(sakura, guioza, 1440, 960, 0);

        assertThat(photo.storagePath())
                .isEqualTo("products/" + sakura.value() + "/" + guioza.value() + "/" + photo.id().value());
        assertThat(photo.fileOf(ImageVariant.THUMB)).isEqualTo(photo.storagePath() + "/thumb.jpg");
        assertThat(photo.type()).isEqualTo(MediaType.IMAGE);
        assertThat(photo.active()).isTrue();
    }

    @Test
    void moveAndDeactivateKeepTheFiles() {
        ProductMedia photo = ProductMedia.newImage(sakura, guioza, 1440, 960, 0);

        ProductMedia changed = photo.at(3).deactivate();

        assertThat(changed.storagePath()).isEqualTo(photo.storagePath());
        assertThat(changed.position()).isEqualTo(3);
        assertThat(changed.active()).isFalse();
    }

    @Test
    void onlyJpegOrPngUpTo8Megabytes() {
        assertThat(new PhotoFile("image/jpeg", 8L * 1024 * 1024).sizeBytes()).isEqualTo(8L * 1024 * 1024);
        assertThatThrownBy(() -> new PhotoFile("image/gif", 1000)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new PhotoFile("image/png", 8L * 1024 * 1024 + 1))
                .isInstanceOf(InvalidValueException.class)
                .hasMessage("A foto precisa ter até 8 MB");
        assertThatThrownBy(() -> new PhotoFile("image/png", 0)).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void aVideoIsBornProcessingAndBecomesReadyWithItsMeasures() {
        ProductMedia video = ProductMedia.newVideo(sakura, guioza, "bunny-123", 2);

        assertThat(video.type()).isEqualTo(MediaType.VIDEO);
        assertThat(video.status()).isEqualTo(MediaStatus.PROCESSING);
        assertThat(video.position()).isEqualTo(2);
        ProductMedia ready = video.converted(1080, 1920, 42);
        assertThat(ready.status()).isEqualTo(MediaStatus.READY);
        assertThat(ready.durationSeconds()).isEqualTo(42);
        assertThat(ready.width()).isEqualTo(1080);
        // Só o vídeo em conversão muda de status.
        assertThatThrownBy(() -> ready.failed("x")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> ProductMedia.newImage(sakura, guioza, 10, 10, 0).failed("x"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void aVideoLongerThanAMinuteIsNotAReels() {
        ProductMedia tooLong = ProductMedia.newVideo(sakura, guioza, "bunny-123", 0).converted(1080, 1920, 61);

        assertThat(tooLong.status()).isEqualTo(MediaStatus.FAILED);
        assertThat(tooLong.failure()).isEqualTo("O vídeo tem 61 segundos. Envie um de até 60.");
        assertThat(ProductMedia.newVideo(sakura, guioza, "b", 0).converted(1080, 1920, 60).status())
                .isEqualTo(MediaStatus.READY);
    }

    @Test
    void onlyMp4MovOrWebmUpTo200Megabytes() {
        assertThat(new VideoFile("video/quicktime", 200L * 1024 * 1024).sizeBytes()).isEqualTo(200L * 1024 * 1024);
        assertThatThrownBy(() -> new VideoFile("video/x-msvideo", 1000)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new VideoFile("video/mp4", 200L * 1024 * 1024 + 1))
                .isInstanceOf(InvalidValueException.class)
                .hasMessage("O vídeo precisa ter até 200 MB");
    }
}
