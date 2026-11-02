package br.com.cardapio.application.service.brand;

import br.com.cardapio.application.port.in.brand.BrandSummary;
import br.com.cardapio.application.port.in.brand.SetLogoUseCase;
import br.com.cardapio.application.port.out.brand.BrandPort;
import br.com.cardapio.application.port.out.media.ImageProcessorPort;
import br.com.cardapio.application.port.out.media.MediaStoragePort;
import br.com.cardapio.application.port.out.media.ProcessedLogo;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.brand.model.Brand;
import br.com.cardapio.domain.brand.model.LogoFile;
import br.com.cardapio.domain.brand.model.LogoVariant;
import br.com.cardapio.domain.shared.exception.UploadNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** A mesma sequência das fotos (Guia 10): ler, conferir, gravar os arquivos, gravar o banco, apagar o original. */
public class SetLogoService implements SetLogoUseCase {

    private final BrandPort brands;
    private final MediaStoragePort storage;
    private final ImageProcessorPort images;

    public SetLogoService(BrandPort brands, MediaStoragePort storage, ImageProcessorPort images) {
        this.brands = brands;
        this.storage = storage;
        this.images = images;
    }

    @Override
    @Transactional
    public BrandSummary set(EstablishmentId establishmentId, UUID uploadId) {
        String uploadPath = StartLogoUploadService.uploadPath(establishmentId, uploadId);
        byte[] original = storage.read(uploadPath, LogoFile.MAX_BYTES).orElseThrow(UploadNotFoundException::new);
        ProcessedLogo logo = images.processLogo(original);

        Brand brand = brands.find(establishmentId).withNewLogo();
        Map<String, byte[]> files = new LinkedHashMap<>();
        for (LogoVariant variant : LogoVariant.values()) {
            files.put(brand.logoFile(variant), logo.variants().get(variant));
        }
        storage.storeAll(files, "image/png"); // os dois tamanhos ao mesmo tempo
        brands.save(brand);
        storage.delete(uploadPath);
        return BrandMapper.toSummary(brand, storage);
    }
}
