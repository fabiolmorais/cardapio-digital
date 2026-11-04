package br.com.cardapio.domain.menu.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.shared.exception.InvalidValueException;

import java.util.Objects;

/**
 * Uma foto ou um vídeo curto de um produto, na galeria do prato. A primeira FOTO da ordem é a
 * capa (a lista do cardápio mostra foto, e não vídeo).
 *
 * @param storagePath     pasta da mídia no armazenamento; cada tamanho da foto é um arquivo dentro dela
 * @param width           largura (da foto grande, ou do vídeo convertido); 0 no vídeo ainda em conversão
 * @param height          altura, idem
 * @param status          se já pode aparecer no cardápio (a foto nasce pronta)
 * @param videoId         o id do vídeo no serviço de vídeo; null na foto
 * @param durationSeconds a duração do vídeo pronto; null na foto e no vídeo em conversão
 * @param failure         por que o vídeo não ficou pronto; null se não falhou
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
        boolean active,
        MediaStatus status,
        String videoId,
        Integer durationSeconds,
        String failure
) {

    /** Fotos e vídeos por produto: a galeria do prato fica curta e o armazenamento não dispara. */
    public static final int MAX_PER_PRODUCT = 8;
    /** Vídeos por produto, entre as mídias acima: o vídeo é o que mais custa guardar e entregar. */
    public static final int MAX_VIDEOS_PER_PRODUCT = 3;
    /** Um Reels: vídeo curto. Mais que isso, o cliente não assiste na mesa. */
    public static final int MAX_VIDEO_SECONDS = 60;

    public ProductMedia {
        Objects.requireNonNull(id);
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(productId);
        Objects.requireNonNull(type);
        Objects.requireNonNull(storagePath);
        Objects.requireNonNull(status);
        if (position < 0) {
            throw new IllegalArgumentException("posição não pode ser negativa");
        }
        if (type == MediaType.IMAGE && (videoId != null || status != MediaStatus.READY)) {
            throw new IllegalArgumentException("a foto nasce pronta e não tem vídeo");
        }
        if (type == MediaType.VIDEO && videoId == null) {
            throw new IllegalArgumentException("o vídeo precisa do id no serviço de vídeo");
        }
        boolean measured = width > 0 && height > 0;
        if (status == MediaStatus.READY && (!measured || (type == MediaType.VIDEO && durationSeconds == null))) {
            throw new IllegalArgumentException("mídia pronta precisa das medidas (e o vídeo, da duração)");
        }
    }

    /**
     * Foto nova, no fim da galeria. A pasta leva o restaurante e o produto: dá para achar (e,
     * se um dia for preciso, apagar) tudo de um restaurante pelo começo do caminho.
     */
    public static ProductMedia newImage(EstablishmentId establishmentId, ProductId productId, int width, int height,
                                        int position) {
        ProductMediaId id = ProductMediaId.newId();
        return new ProductMedia(id, establishmentId, productId, MediaType.IMAGE, folder(establishmentId, productId, id),
                width, height, position, true, MediaStatus.READY, null, null, null);
    }

    /**
     * Vídeo novo, no fim da galeria, em conversão: o arquivo vai direto do navegador para o
     * serviço de vídeo, que avisa quando terminar.
     */
    public static ProductMedia newVideo(EstablishmentId establishmentId, ProductId productId, String videoId,
                                        int position) {
        ProductMediaId id = ProductMediaId.newId();
        return new ProductMedia(id, establishmentId, productId, MediaType.VIDEO, folder(establishmentId, productId, id),
                0, 0, position, true, MediaStatus.PROCESSING, Objects.requireNonNull(videoId), null, null);
    }

    private static String folder(EstablishmentId establishmentId, ProductId productId, ProductMediaId id) {
        return "products/" + establishmentId.value() + "/" + productId.value() + "/" + id.value();
    }

    public boolean isVideo() {
        return type == MediaType.VIDEO;
    }

    /** Caminho do arquivo de um dos tamanhos da foto. */
    public String fileOf(ImageVariant variant) {
        return storagePath + "/" + variant.fileName();
    }

    /**
     * O serviço de vídeo terminou a conversão. Passou de 60 segundos? Não serve como Reels: fica
     * como falha, com o motivo, para o dono enviar um mais curto.
     */
    public ProductMedia converted(int videoWidth, int videoHeight, int seconds) {
        requireProcessingVideo();
        if (videoWidth <= 0 || videoHeight <= 0 || seconds <= 0) {
            throw new InvalidValueException("O serviço de vídeo devolveu medidas inválidas");
        }
        if (seconds > MAX_VIDEO_SECONDS) {
            return failed("O vídeo tem " + seconds + " segundos. Envie um de até " + MAX_VIDEO_SECONDS + ".");
        }
        return new ProductMedia(id, establishmentId, productId, type, storagePath, videoWidth, videoHeight, position,
                active, MediaStatus.READY, videoId, seconds, null);
    }

    /** A conversão falhou (o arquivo não era um vídeo de verdade, por exemplo). */
    public ProductMedia failed(String reason) {
        requireProcessingVideo();
        return new ProductMedia(id, establishmentId, productId, type, storagePath, 0, 0, position, active,
                MediaStatus.FAILED, videoId, null, Objects.requireNonNull(reason));
    }

    private void requireProcessingVideo() {
        if (!isVideo() || status != MediaStatus.PROCESSING) {
            throw new IllegalStateException("só o vídeo em conversão muda de status");
        }
    }

    public ProductMedia at(int newPosition) {
        return new ProductMedia(id, establishmentId, productId, type, storagePath, width, height, newPosition, active,
                status, videoId, durationSeconds, failure);
    }

    /** O registro fica, como todo cadastro. Os arquivos da foto também (a limpeza vem com a Etapa 15). */
    public ProductMedia deactivate() {
        return new ProductMedia(id, establishmentId, productId, type, storagePath, width, height, position, false,
                status, videoId, durationSeconds, failure);
    }
}
