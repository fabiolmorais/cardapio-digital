package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.in.menu.MediaUpload;
import br.com.cardapio.application.port.in.menu.ProductMediaSummary;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.exception.MediaLimitReachedException;
import br.com.cardapio.domain.menu.exception.ProductMediaNotFoundException;
import br.com.cardapio.domain.menu.exception.ProductNotFoundException;
import br.com.cardapio.domain.menu.model.ImageVariant;
import br.com.cardapio.domain.menu.model.MenuCategory;
import br.com.cardapio.domain.menu.model.PhotoFile;
import br.com.cardapio.domain.menu.model.Product;
import br.com.cardapio.domain.menu.model.ProductMedia;
import br.com.cardapio.domain.shared.exception.InvalidImageException;
import br.com.cardapio.domain.shared.exception.OutdatedListException;
import br.com.cardapio.domain.shared.exception.UploadNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MediaServicesTest {

    private final MenuFakes menu = new MenuFakes();
    private final MediaFakes fakes = new MediaFakes();
    private final StartMediaUploadService start = new StartMediaUploadService(menu, fakes, fakes);
    private final AddProductMediaService add = new AddProductMediaService(menu, fakes, fakes, fakes);
    private final ListProductMediaService list = new ListProductMediaService(menu, fakes, fakes, fakes);
    private final RemoveProductMediaService remove = new RemoveProductMediaService(fakes, fakes);
    private final ReorderProductMediaService reorder = new ReorderProductMediaService(menu, fakes);
    private final EstablishmentId sakura = EstablishmentId.newId();
    private final PhotoFile jpeg = new PhotoFile("image/jpeg", 2_000_000);
    private Product guioza;

    @BeforeEach
    void menu() {
        MenuCategory entradas = menu.givenCategory(sakura, "Entradas");
        guioza = menu.givenProduct(entradas, "Guioza", 2990);
    }

    /** Os dois passos, com o navegador enviando o arquivo no meio. */
    private ProductMediaSummary uploadPhoto(String content) {
        MediaUpload upload = start.start(sakura, guioza.id(), jpeg);
        fakes.browserUploads(upload.url(), content.getBytes());
        return add.add(sakura, guioza.id(), upload.uploadId());
    }

    @Test
    void theUploadUrlPointsToAFolderOfThisRestaurantAndProduct() {
        MediaUpload upload = start.start(sakura, guioza.id(), jpeg);

        assertThat(upload.url()).isEqualTo("https://r2.test/uploads/" + sakura.value() + "/" + guioza.id().value()
                + "/" + upload.uploadId());
        assertThat(upload.headers()).containsEntry("Content-Type", "image/jpeg");
    }

    @Test
    void addingAPhotoStoresTheThreeSizesAndDropsTheOriginal() {
        ProductMediaSummary photo = uploadPhoto("FOTO do guioza");

        String folder = fakes.media.get(photo.id()).storagePath();
        assertThat(fakes.files).containsOnlyKeys(folder + "/thumb.jpg", folder + "/medium.jpg", folder + "/large.jpg");
        assertThat(photo.urls()).containsEntry(ImageVariant.LARGE, "https://fotos.test/" + folder + "/large.jpg");
        assertThat(photo.position()).isZero();
        assertThat(uploadPhoto("FOTO 2").position()).isEqualTo(1);
    }

    @Test
    void anUploadThatNeverArrivedOrIsNotAPhotoIsRefused() {
        MediaUpload neverSent = start.start(sakura, guioza.id(), jpeg);
        assertThatThrownBy(() -> add.add(sakura, guioza.id(), neverSent.uploadId()))
                .isInstanceOf(UploadNotFoundException.class);

        assertThatThrownBy(() -> uploadPhoto("<script>alert(1)</script>")).isInstanceOf(InvalidImageException.class);
        assertThat(fakes.media).isEmpty();
    }

    @Test
    void eightPhotosAtMost() {
        for (int i = 0; i < ProductMedia.MAX_PER_PRODUCT; i++) {
            uploadPhoto("FOTO " + i);
        }
        assertThatThrownBy(() -> start.start(sakura, guioza.id(), jpeg)).isInstanceOf(MediaLimitReachedException.class);

        remove.remove(sakura, guioza.id(), list.list(sakura, guioza.id()).content().getFirst().id());
        assertThat(start.start(sakura, guioza.id(), jpeg).uploadId()).isNotNull();
    }

    @Test
    void removeAndReorder() {
        ProductMediaSummary first = uploadPhoto("FOTO 1");
        ProductMediaSummary second = uploadPhoto("FOTO 2");
        ProductMediaSummary third = uploadPhoto("FOTO 3");

        reorder.reorder(sakura, guioza.id(), List.of(third.id(), first.id(), second.id()));
        assertThat(list.list(sakura, guioza.id()).content()).extracting(ProductMediaSummary::id)
                .containsExactly(third.id(), first.id(), second.id());

        remove.remove(sakura, guioza.id(), first.id());
        remove.remove(sakura, guioza.id(), first.id()); // de novo: tudo bem
        assertThat(list.list(sakura, guioza.id()).content()).extracting(ProductMediaSummary::id)
                .containsExactly(third.id(), second.id());
        assertThatThrownBy(() -> reorder.reorder(sakura, guioza.id(), List.of(second.id(), third.id(), first.id())))
                .isInstanceOf(OutdatedListException.class);
    }

    @Test
    void anotherRestaurantOrAnotherProductCannotTouchThePhotos() {
        ProductMediaSummary photo = uploadPhoto("FOTO");
        EstablishmentId other = EstablishmentId.newId();
        Product sunomono = menu.givenProduct(menu.categories.values().iterator().next(), "Sunomono", 1990);

        assertThatThrownBy(() -> start.start(other, guioza.id(), jpeg)).isInstanceOf(ProductNotFoundException.class);
        assertThatThrownBy(() -> list.list(other, guioza.id())).isInstanceOf(ProductNotFoundException.class);
        assertThatThrownBy(() -> remove.remove(other, guioza.id(), photo.id()))
                .isInstanceOf(ProductMediaNotFoundException.class);
        // A foto do guioza não pode ser removida pela URL do sunomono.
        assertThatThrownBy(() -> remove.remove(sakura, sunomono.id(), photo.id()))
                .isInstanceOf(ProductMediaNotFoundException.class);
        // O uploadId do guioza, usado no sunomono, aponta para uma pasta onde não há nada.
        MediaUpload upload = start.start(sakura, guioza.id(), jpeg);
        fakes.browserUploads(upload.url(), "FOTO".getBytes());
        assertThatThrownBy(() -> add.add(sakura, sunomono.id(), upload.uploadId()))
                .isInstanceOf(UploadNotFoundException.class);
        assertThatThrownBy(() -> add.add(other, guioza.id(), UUID.randomUUID()))
                .isInstanceOf(ProductNotFoundException.class);
    }
}
