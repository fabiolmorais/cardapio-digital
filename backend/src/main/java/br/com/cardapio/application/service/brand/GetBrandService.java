package br.com.cardapio.application.service.brand;

import br.com.cardapio.application.port.in.brand.BrandSummary;
import br.com.cardapio.application.port.in.brand.GetBrandUseCase;
import br.com.cardapio.application.port.out.brand.BrandPort;
import br.com.cardapio.application.port.out.media.MediaStoragePort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import org.springframework.transaction.annotation.Transactional;

public class GetBrandService implements GetBrandUseCase {

    private final BrandPort brands;
    private final MediaStoragePort storage;

    public GetBrandService(BrandPort brands, MediaStoragePort storage) {
        this.brands = brands;
        this.storage = storage;
    }

    @Override
    @Transactional(readOnly = true)
    public BrandSummary get(EstablishmentId establishmentId) {
        return BrandMapper.toSummary(brands.find(establishmentId), storage);
    }
}
