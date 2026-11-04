package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.in.menu.PublicMenu;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.exception.TableNotFoundException;
import br.com.cardapio.domain.menu.model.ImageVariant;
import br.com.cardapio.domain.menu.model.MediaType;
import br.com.cardapio.domain.menu.model.MenuCategory;
import br.com.cardapio.domain.menu.model.Product;
import br.com.cardapio.domain.menu.model.ProductMedia;
import br.com.cardapio.support.TablesByToken;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class GetPublicMenuServiceTest {

    private static final String QR = "qr-da-mesa-04-00000000000";

    private final MenuFakes menu = new MenuFakes();
    private final MediaFakes media = new MediaFakes();
    private final TablesByToken tables = new TablesByToken();
    private final GetPublicMenuService service = new GetPublicMenuService(tables, menu, menu, media, media, media);
    private final EstablishmentId sakura = EstablishmentId.newId();

    @Test
    void categoriesInOrderEachWithItsProductsAndEmptyOnesLeftOut() {
        tables.given(sakura, "04", QR);
        MenuCategory entradas = menu.givenCategory(sakura, "Entradas");
        menu.givenCategory(sakura, "Sobremesas"); // sem produto: não aparece
        MenuCategory bebidas = menu.givenCategory(sakura, "Bebidas");
        menu.givenProduct(bebidas, "Suco", 1000);
        menu.givenProduct(entradas, "Guioza", 2990);
        Product ceviche = menu.givenProduct(entradas, "Ceviche", 3490);
        menu.save(ceviche.markSoldOut());
        Product antigo = menu.givenProduct(entradas, "Prato antigo", 100);
        menu.save(antigo.deactivate());

        PublicMenu result = service.get(QR);

        assertThat(result.categories()).extracting(PublicMenu.Category::name).containsExactly("Entradas", "Bebidas");
        assertThat(result.categories().getFirst().products())
                .extracting(PublicMenu.Item::name, PublicMenu.Item::available)
                .containsExactly(tuple("Guioza", true), tuple("Ceviche", false));
    }

    @Test
    void eachProductCarriesItsCoverPhoto() {
        tables.given(sakura, "04", QR);
        MenuCategory entradas = menu.givenCategory(sakura, "Entradas");
        Product guioza = menu.givenProduct(entradas, "Guioza", 2990);
        menu.givenProduct(entradas, "Sunomono", 1990);
        ProductMedia capa = ProductMedia.newImage(sakura, guioza.id(), 1440, 960, 0);
        media.save(capa);
        media.save(ProductMedia.newImage(sakura, guioza.id(), 1440, 960, 1));

        var items = service.get(QR).categories().getFirst().products();

        assertThat(items.get(0).photoUrls())
                .containsEntry(ImageVariant.MEDIUM, "https://fotos.test/" + capa.fileOf(ImageVariant.MEDIUM));
        assertThat(items.get(1).photoUrls()).isEmpty();
    }

    @Test
    void theQrDecidesTheRestaurantAndAnUnknownQrIsNotFound() {
        tables.given(sakura, "04", QR);
        menu.givenProduct(menu.givenCategory(EstablishmentId.newId(), "Deles"), "Prato de outro restaurante", 100);

        assertThat(service.get(QR).categories()).isEmpty();
        assertThatThrownBy(() -> service.get("qr-que-ninguem-gerou-0000")).isInstanceOf(TableNotFoundException.class);
    }

    @Test
    void eachProductCarriesItsReadyGalleryInOrderWithThePhotoAsCover() {
        tables.given(sakura, "04", QR);
        MenuCategory entradas = menu.givenCategory(sakura, "Entradas");
        Product guioza = menu.givenProduct(entradas, "Guioza", 2990);
        // O vídeo pronto vem antes na galeria, mas a capa continua sendo a foto.
        media.save(ProductMedia.newVideo(sakura, guioza.id(), "pronto", 0).converted(1080, 1920, 18));
        ProductMedia photo = ProductMedia.newImage(sakura, guioza.id(), 1440, 960, 1);
        media.save(photo);
        media.save(ProductMedia.newVideo(sakura, guioza.id(), "convertendo", 2));
        media.save(ProductMedia.newVideo(sakura, guioza.id(), "falhou", 3).failed("não deu"));
        media.save(ProductMedia.newImage(sakura, guioza.id(), 1440, 960, 4).deactivate());

        PublicMenu.Item item = service.get(QR).categories().getFirst().products().getFirst();

        assertThat(item.photoUrls())
                .containsEntry(ImageVariant.THUMB, "https://fotos.test/" + photo.fileOf(ImageVariant.THUMB));
        assertThat(item.media()).extracting(PublicMenu.Media::type).containsExactly(MediaType.VIDEO, MediaType.IMAGE);
        assertThat(item.media().getFirst().videoUrl()).isEqualTo("https://video.test/pronto.mp4");
        assertThat(item.media().getFirst().durationSeconds()).isEqualTo(18);
        assertThat(item.media().getFirst().imageUrls()).isEmpty();
        assertThat(item.media().get(1).videoUrl()).isNull();
    }
}
