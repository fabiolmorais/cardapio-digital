package br.com.cardapio.application.port.in.menu;

import br.com.cardapio.domain.menu.model.ImageVariant;
import br.com.cardapio.domain.menu.model.MediaType;
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
     * @param photoUrls       os tamanhos da capa (a primeira foto); vazio quando o produto não tem foto
     * @param media           a galeria pronta, em ordem: fotos e vídeos (o Reels do prato)
     */
    /** @param aLaCarte false: só do rodízio (a mesa mostra só na aba Rodízio) */
    public record Item(ProductId id, String name, String description, long priceCents, Long promoPriceCents,
                       boolean featured, boolean available, boolean aLaCarte, Map<ImageVariant, String> photoUrls,
                       List<Media> media) {

        public Item {
            photoUrls = Map.copyOf(photoUrls);
            media = List.copyOf(media);
        }
    }

    /**
     * Uma mídia da galeria do prato. Foto: os tamanhos. Vídeo: o endereço para tocar, a capa e a
     * duração.
     *
     * @param imageUrls       os tamanhos da foto; vazio no vídeo
     * @param videoUrl        o vídeo, para tocar; null na foto
     * @param posterUrl       a capa do vídeo; null na foto, ou se o serviço não gera
     * @param durationSeconds a duração do vídeo; null na foto
     */
    public record Media(MediaType type, int width, int height, Map<ImageVariant, String> imageUrls, String videoUrl,
                        String posterUrl, Integer durationSeconds) {

        public Media {
            imageUrls = Map.copyOf(imageUrls);
        }
    }
}
