package br.com.cardapio.adapter.out.persistence.account;

import br.com.cardapio.application.port.out.brand.BrandPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.brand.model.Brand;
import br.com.cardapio.domain.brand.model.BrandColor;
import br.com.cardapio.domain.brand.model.InstagramHandle;
import br.com.cardapio.domain.brand.model.ReviewLink;
import br.com.cardapio.domain.brand.model.Theme;
import org.springframework.stereotype.Component;

/**
 * A marca mora na tabela establishment. Por isso o adaptador fica neste pacote, ao lado da
 * entidade (que não é pública). O filtro por restaurante do Hibernate não se aplica aqui: o
 * restaurante É a linha, e a busca é sempre pelo id que veio do token.
 */
@Component
class BrandPersistenceAdapter implements BrandPort {

    private final EstablishmentJpaRepository establishments;

    BrandPersistenceAdapter(EstablishmentJpaRepository establishments) {
        this.establishments = establishments;
    }

    @Override
    public Brand find(EstablishmentId establishmentId) {
        EstablishmentJpaEntity e = load(establishmentId);
        return new Brand(establishmentId, e.getName(), BrandColor.valueOf(e.getBrandColor()),
                Theme.valueOf(e.getTheme()), e.getLogoPath(),
                e.getInstagram() == null ? null : new InstagramHandle(e.getInstagram()),
                e.getGoogleReviewUrl() == null ? null : new ReviewLink(e.getGoogleReviewUrl()));
    }

    @Override
    public void save(Brand brand) {
        load(brand.establishmentId()).applyBrand(brand.name(), brand.color().name(), brand.theme().name(),
                brand.logoPath(), brand.instagram() == null ? null : brand.instagram().value(),
                brand.reviewLink() == null ? null : brand.reviewLink().value());
    }

    private EstablishmentJpaEntity load(EstablishmentId establishmentId) {
        return establishments.findById(establishmentId.value())
                .orElseThrow(() -> new IllegalStateException("restaurante do token não existe"));
    }
}
