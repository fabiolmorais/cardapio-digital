package br.com.cardapio.domain.menu.model;

import br.com.cardapio.domain.account.model.EstablishmentId;

import java.util.Objects;

/**
 * Uma foto (mais tarde, também um vídeo) de um produto. A primeira da ordem é a capa.
 *
 * @param storagePath pasta da mídia no armazenamento; cada tamanho é um arquivo dentro dela
 * @param width       largura do tamanho grande, em pixels
 * @param height      altura do tamanho grande, em pixels
 */
public record ProductMedia(
        ProductMediaId id,
        EstablishmentId establishmentId,
        ProductId productId,
        MediaType type,
        String storagePath,
        int width,
        int height,
        int position,
        boolean active
) {

    /** Fotos por produto: a galeria do prato fica curta e o armazenamento não dispara. */
    public static final int MAX_PER_PRODUCT = 8;

    public ProductMedia {
        Objects.requireNonNull(id);
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(productId);
        Objects.requireNonNull(type);
        Objects.requireNonNull(storagePath);
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("medidas da mídia precisam ser positivas");
        }
        if (position < 0) {
            throw new IllegalArgumentException("posição não pode ser negativa");
        }
    }

    /**
     * Foto nova, no fim da galeria. A pasta leva o restaurante e o produto: dá para achar (e,
     * se um dia for preciso, apagar) tudo de um restaurante pelo começo do caminho.
     */
    public static ProductMedia newImage(EstablishmentId establishmentId, ProductId productId, int width, int height,
                                        int position) {
        ProductMediaId id = ProductMediaId.newId();
        String path = "products/" + establishmentId.value() + "/" + productId.value() + "/" + id.value();
        return new ProductMedia(id, establishmentId, productId, MediaType.IMAGE, path, width, height, position, true);
    }

    /** Caminho do arquivo de um dos tamanhos. */
    public String fileOf(ImageVariant variant) {
        return storagePath + "/" + variant.fileName();
    }

    public ProductMedia at(int newPosition) {
        return new ProductMedia(id, establishmentId, productId, type, storagePath, width, height, newPosition, active);
    }

    /** O registro fica, como todo cadastro; os arquivos também (a limpeza vem com a Etapa 15). */
    public ProductMedia deactivate() {
        return new ProductMedia(id, establishmentId, productId, type, storagePath, width, height, position, false);
    }
}
