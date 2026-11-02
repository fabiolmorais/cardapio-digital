package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.in.menu.AddProductMediaUseCase;
import br.com.cardapio.application.port.in.menu.ProductMediaSummary;
import br.com.cardapio.application.port.out.media.ImageProcessorPort;
import br.com.cardapio.application.port.out.media.MediaStoragePort;
import br.com.cardapio.application.port.out.media.ProcessedImage;
import br.com.cardapio.application.port.out.menu.ProductMediaPort;
import br.com.cardapio.application.port.out.menu.ProductPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.exception.MediaLimitReachedException;
import br.com.cardapio.domain.shared.exception.UploadNotFoundException;
import br.com.cardapio.domain.menu.model.ImageVariant;
import br.com.cardapio.domain.menu.model.PhotoFile;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.menu.model.ProductMedia;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public class AddProductMediaService implements AddProductMediaUseCase {

    private final ProductPort products;
    private final ProductMediaPort media;
    private final MediaStoragePort storage;
    private final ImageProcessorPort images;

    public AddProductMediaService(ProductPort products, ProductMediaPort media, MediaStoragePort storage,
                                  ImageProcessorPort images) {
        this.products = products;
        this.media = media;
        this.storage = storage;
        this.images = images;
    }

    @Override
    @Transactional
    public ProductMediaSummary add(EstablishmentId establishmentId, ProductId productId, UUID uploadId) {
        MenuLookups.activeProduct(products, establishmentId, productId);
        if (media.findActiveByProduct(establishmentId, productId).size() >= ProductMedia.MAX_PER_PRODUCT) {
            throw new MediaLimitReachedException();
        }

        String uploadPath = StartMediaUploadService.uploadPath(establishmentId, productId, uploadId);
        byte[] original = storage.read(uploadPath, PhotoFile.MAX_BYTES).orElseThrow(UploadNotFoundException::new);
        ProcessedImage image = images.process(original); // confere o tipo real; InvalidImageException se não for foto

        ProductMedia photo = ProductMedia.newImage(establishmentId, productId, image.width(), image.height(),
                media.nextPosition(establishmentId, productId));
        for (ImageVariant variant : ImageVariant.values()) {
            storage.store(photo.fileOf(variant), image.variants().get(variant), "image/jpeg");
        }
        media.save(photo);
        storage.delete(uploadPath); // o original (com os metadados do celular) não fica guardado
        return ProductMediaMapper.toSummary(photo, storage);
    }
}
