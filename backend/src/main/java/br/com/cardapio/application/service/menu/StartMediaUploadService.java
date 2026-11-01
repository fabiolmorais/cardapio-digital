package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.in.menu.MediaUpload;
import br.com.cardapio.application.port.in.menu.StartMediaUploadUseCase;
import br.com.cardapio.application.port.out.media.MediaStoragePort;
import br.com.cardapio.application.port.out.menu.ProductMediaPort;
import br.com.cardapio.application.port.out.menu.ProductPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.exception.MediaLimitReachedException;
import br.com.cardapio.domain.menu.model.PhotoFile;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.menu.model.ProductMedia;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

public class StartMediaUploadService implements StartMediaUploadUseCase {

    /** Tempo para o navegador começar o envio. Curto: a URL é uma permissão de escrita. */
    static final Duration UPLOAD_VALIDITY = Duration.ofMinutes(5);

    private final ProductPort products;
    private final ProductMediaPort media;
    private final MediaStoragePort storage;

    public StartMediaUploadService(ProductPort products, ProductMediaPort media, MediaStoragePort storage) {
        this.products = products;
        this.media = media;
        this.storage = storage;
    }

    /**
     * O caminho do envio leva o restaurante e o produto de quem pediu. No segundo passo, o
     * caminho é montado de novo com os dados do token: um uploadId de outro restaurante
     * aponta para um arquivo que não existe.
     */
    static String uploadPath(EstablishmentId establishmentId, ProductId productId, UUID uploadId) {
        return "uploads/" + establishmentId.value() + "/" + productId.value() + "/" + uploadId;
    }

    @Override
    @Transactional(readOnly = true)
    public MediaUpload start(EstablishmentId establishmentId, ProductId productId, PhotoFile file) {
        MenuLookups.activeProduct(products, establishmentId, productId);
        if (media.findActiveByProduct(establishmentId, productId).size() >= ProductMedia.MAX_PER_PRODUCT) {
            throw new MediaLimitReachedException();
        }
        UUID uploadId = UUID.randomUUID();
        var signed = storage.signUpload(uploadPath(establishmentId, productId, uploadId), file.contentType(),
                file.sizeBytes(), UPLOAD_VALIDITY);
        return new MediaUpload(uploadId, signed.url(), signed.headers(), signed.expiresAt());
    }
}
