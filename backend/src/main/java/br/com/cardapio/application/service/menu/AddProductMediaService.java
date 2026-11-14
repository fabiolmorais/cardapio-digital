package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.in.menu.AddProductMediaUseCase;
import br.com.cardapio.application.port.in.menu.ProductMediaSummary;
import br.com.cardapio.application.port.out.media.ImageProcessorPort;
import br.com.cardapio.application.port.out.media.MediaStoragePort;
import br.com.cardapio.application.port.out.media.ProcessedImage;
import br.com.cardapio.application.port.out.menu.ProductMediaPort;
import br.com.cardapio.application.port.out.menu.OptionGroupPort;
import br.com.cardapio.application.port.out.menu.ProductPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.exception.MediaLimitReachedException;
import br.com.cardapio.domain.shared.exception.UploadNotFoundException;
import br.com.cardapio.domain.menu.model.ImageVariant;
import br.com.cardapio.domain.menu.model.PhotoFile;
import br.com.cardapio.domain.menu.model.MediaOwner;
import br.com.cardapio.domain.menu.model.ProductMedia;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class AddProductMediaService implements AddProductMediaUseCase {

    private final ProductPort products;
    private final OptionGroupPort groups;
    private final ProductMediaPort media;
    private final MediaStoragePort storage;
    private final ImageProcessorPort images;

    public AddProductMediaService(ProductPort products, OptionGroupPort groups, ProductMediaPort media, MediaStoragePort storage,
                                  ImageProcessorPort images) {
        this.products = products;
        this.groups = groups;
        this.media = media;
        this.storage = storage;
        this.images = images;
    }

    @Override
    @Transactional
    public ProductMediaSummary add(EstablishmentId establishmentId, MediaOwner owner, UUID uploadId) {
        MediaOwners.requireActive(products, groups, establishmentId, owner);
        if (media.findActiveByOwner(establishmentId, owner).size() >= ProductMedia.MAX_PER_PRODUCT) {
            throw new MediaLimitReachedException();
        }

        String uploadPath = StartMediaUploadService.uploadPath(establishmentId, owner, uploadId);
        byte[] original = storage.read(uploadPath, PhotoFile.MAX_BYTES).orElseThrow(UploadNotFoundException::new);
        ProcessedImage image = images.process(original); // confere o tipo real; InvalidImageException se não for foto

        ProductMedia photo = ProductMedia.newImage(establishmentId, owner, image.width(), image.height(),
                media.nextPosition(establishmentId, owner));
        Map<String, byte[]> files = new LinkedHashMap<>();
        for (ImageVariant variant : ImageVariant.values()) {
            files.put(photo.fileOf(variant), image.variants().get(variant));
        }
        storage.storeAll(files, "image/jpeg"); // os três tamanhos ao mesmo tempo
        media.save(photo);
        storage.delete(uploadPath); // o original (com os metadados do celular) não fica guardado
        return ProductMediaMapper.toSummary(photo, storage);
    }
}
