package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.in.menu.GetPublicMenuUseCase;
import br.com.cardapio.application.port.in.menu.PublicMenu;
import br.com.cardapio.application.port.out.floor.DiningTablePort;
import br.com.cardapio.application.port.out.media.MediaStoragePort;
import br.com.cardapio.application.port.out.menu.MenuCategoryPort;
import br.com.cardapio.application.port.out.menu.ProductMediaPort;
import br.com.cardapio.application.port.out.menu.ProductPort;
import br.com.cardapio.application.port.out.video.VideoHostingPort;
import br.com.cardapio.application.service.floor.PublicTableLookup;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.ImageVariant;
import br.com.cardapio.domain.menu.model.MenuCategoryId;
import br.com.cardapio.domain.menu.model.Product;
import br.com.cardapio.domain.menu.model.ProductDetails;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.menu.model.ProductMedia;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Monta o cardápio da página da mesa com três consultas, qualquer que seja o tamanho dele:
 * as categorias, todos os produtos e todas as galerias prontas (de onde sai também a capa).
 * Juntar em memória é o que evita o N+1 (uma consulta de mídia por produto, que com 200
 * produtos seriam 200 idas ao banco).
 */
public class GetPublicMenuService implements GetPublicMenuUseCase {

    private final DiningTablePort tables;
    private final MenuCategoryPort categories;
    private final ProductPort products;
    private final ProductMediaPort media;
    private final MediaStoragePort storage;
    private final VideoHostingPort videos;

    public GetPublicMenuService(DiningTablePort tables, MenuCategoryPort categories, ProductPort products,
                                ProductMediaPort media, MediaStoragePort storage, VideoHostingPort videos) {
        this.tables = tables;
        this.categories = categories;
        this.products = products;
        this.media = media;
        this.storage = storage;
        this.videos = videos;
    }

    @Override
    @Transactional(readOnly = true)
    public PublicMenu get(String tableToken) {
        EstablishmentId establishmentId = PublicTableLookup.resolve(tables, tableToken).table().establishmentId();

        // A consulta já vem em ordem (produto, posição): o groupingBy mantém a ordem de cada galeria.
        Map<ProductId, List<ProductMedia>> galleries = media.findReadyGalleries(establishmentId).stream()
                .collect(Collectors.groupingBy(ProductMedia::productId));
        Map<MenuCategoryId, List<PublicMenu.Item>> byCategory = products.findAllActive(establishmentId).stream()
                .collect(Collectors.groupingBy(Product::categoryId,
                        Collectors.mapping(p -> item(p, galleries.getOrDefault(p.id(), List.of())),
                                Collectors.toList())));

        List<PublicMenu.Category> visible = categories.findActive(establishmentId).stream()
                .filter(c -> byCategory.containsKey(c.id()))
                .map(c -> new PublicMenu.Category(c.id(), c.name(), byCategory.get(c.id())))
                .toList();
        return new PublicMenu(visible);
    }

    private PublicMenu.Item item(Product product, List<ProductMedia> gallery) {
        ProductDetails d = product.details();
        // A capa é a primeira FOTO: a lista do cardápio mostra foto, e não vídeo.
        Map<ImageVariant, String> cover = gallery.stream().filter(m -> !m.isVideo()).findFirst()
                .map(this::imageUrls).orElse(Map.of());
        return new PublicMenu.Item(product.id(), d.name(), d.description(), d.price().cents(),
                d.onSale() ? d.promoPrice().cents() : null, d.featured(), product.available(), d.aLaCarte(), cover,
                gallery.stream().map(this::media).toList());
    }

    private PublicMenu.Media media(ProductMedia m) {
        return m.isVideo()
                ? new PublicMenu.Media(m.type(), m.width(), m.height(), Map.of(), videos.playbackUrl(m.videoId()),
                        videos.posterUrl(m.videoId()), m.durationSeconds())
                : new PublicMenu.Media(m.type(), m.width(), m.height(), imageUrls(m), null, null, null);
    }

    private Map<ImageVariant, String> imageUrls(ProductMedia photo) {
        return Arrays.stream(ImageVariant.values())
                .collect(Collectors.toMap(Function.identity(), v -> storage.publicUrl(photo.fileOf(v))));
    }
}
