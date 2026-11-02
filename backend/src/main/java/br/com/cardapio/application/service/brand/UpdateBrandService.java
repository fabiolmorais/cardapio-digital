package br.com.cardapio.application.service.brand;

import br.com.cardapio.application.port.in.brand.BrandSummary;
import br.com.cardapio.application.port.in.brand.UpdateBrandCommand;
import br.com.cardapio.application.port.in.brand.UpdateBrandUseCase;
import br.com.cardapio.application.port.out.brand.BrandPort;
import br.com.cardapio.application.port.out.media.MediaStoragePort;
import br.com.cardapio.domain.brand.model.Brand;
import br.com.cardapio.domain.brand.model.InstagramHandle;
import br.com.cardapio.domain.brand.model.ReviewLink;
import org.springframework.transaction.annotation.Transactional;

public class UpdateBrandService implements UpdateBrandUseCase {

    private final BrandPort brands;
    private final MediaStoragePort storage;

    public UpdateBrandService(BrandPort brands, MediaStoragePort storage) {
        this.brands = brands;
        this.storage = storage;
    }

    @Override
    @Transactional
    public BrandSummary update(UpdateBrandCommand command) {
        Brand brand = brands.find(command.establishmentId()).edit(command.name(), command.color(), command.theme(),
                InstagramHandle.parse(command.instagram()), ReviewLink.parse(command.reviewLink()));
        brands.save(brand);
        return BrandMapper.toSummary(brand, storage);
    }
}
