package br.com.cardapio.application.service.brand;

import br.com.cardapio.application.port.in.brand.BrandSummary;
import br.com.cardapio.application.port.in.brand.RemoveLogoUseCase;
import br.com.cardapio.application.port.out.brand.BrandPort;
import br.com.cardapio.application.port.out.media.MediaStoragePort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.brand.model.Brand;
import org.springframework.transaction.annotation.Transactional;

public class RemoveLogoService implements RemoveLogoUseCase {

    private final BrandPort brands;
    private final MediaStoragePort storage;

    public RemoveLogoService(BrandPort brands, MediaStoragePort storage) {
        this.brands = brands;
        this.storage = storage;
    }

    @Override
    @Transactional
    public BrandSummary remove(EstablishmentId establishmentId) {
        Brand brand = brands.find(establishmentId).withoutLogo();
        brands.save(brand);
        return BrandMapper.toSummary(brand, storage);
    }
}
