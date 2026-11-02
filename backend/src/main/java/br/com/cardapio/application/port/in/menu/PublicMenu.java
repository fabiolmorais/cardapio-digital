package br.com.cardapio.application.port.in.menu;

import br.com.cardapio.domain.menu.model.ImageVariant;
import br.com.cardapio.domain.menu.model.MenuCategoryId;
import br.com.cardapio.domain.menu.model.ProductId;

import java.util.List;
import java.util.Map;

/**
 * O cardápio como o cliente vê: as categorias na ordem, cada uma com os produtos dela.
 * Categoria sem produto ativo não aparece. Produto esgotado aparece (com available = false).
 */
public record PublicMenu(List<Category> categories) {

    public PublicMenu {
        categories = List.copyOf(categories);
    }

    public record Category(MenuCategoryId id, String name, List<Item> products) {

        public Category {
            products = List.copyOf(products);
        }
    }

    /**
     * @param promoPriceCents null quando não há promoção
     * @param photoUrls       os tamanhos da capa; vazio quando o produto não tem foto
     */
    public record Item(ProductId id, String name, String description, long priceCents, Long promoPriceCents,
                       boolean featured, boolean available, Map<ImageVariant, String> photoUrls) {

        public Item {
            photoUrls = Map.copyOf(photoUrls);
        }
    }
}
