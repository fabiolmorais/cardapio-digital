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
}
