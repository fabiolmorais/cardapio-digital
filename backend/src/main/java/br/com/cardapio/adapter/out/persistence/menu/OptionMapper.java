package br.com.cardapio.adapter.out.persistence.menu;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.OptionGroup;
import br.com.cardapio.domain.menu.model.OptionGroupId;
import br.com.cardapio.domain.menu.model.OptionGroupRules;
import br.com.cardapio.domain.menu.model.OptionItem;
import br.com.cardapio.domain.menu.model.OptionItemId;
import br.com.cardapio.domain.menu.model.OptionPricing;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.menu.model.ProductVariant;
import br.com.cardapio.domain.menu.model.ProductVariantId;
import br.com.cardapio.domain.shared.model.Money;

/** Entidade JPA ↔ domínio, para os tamanhos, os grupos e as opções. */
final class OptionMapper {

    private OptionMapper() {
    }

    static OptionGroup toDomain(OptionGroupJpaEntity e) {
        return new OptionGroup(new OptionGroupId(e.getId()), new EstablishmentId(e.getEstablishmentId()), e.getName(),
                e.getTitle(), new OptionGroupRules(e.getMinChoices(), e.getMaxChoices(), e.getMaxPerOption(),
                e.isSplitsItem(), OptionPricing.valueOf(e.getPricing()), e.getFreeQuantity()), e.isActive());
    }

    static OptionGroupJpaEntity toEntity(OptionGroup g) {
        OptionGroupRules r = g.rules();
        return new OptionGroupJpaEntity(g.id().value(), g.establishmentId().value(), g.name(), g.title(),
                r.minChoices(), r.maxChoices(), r.maxPerOption(), r.splitsItem(), r.pricing().name(),
                r.freeQuantity(), g.active());
    }

    static OptionItem toDomain(OptionItemJpaEntity e) {
        return new OptionItem(new OptionItemId(e.getId()), new EstablishmentId(e.getEstablishmentId()),
                new OptionGroupId(e.getOptionGroupId()), e.getName(), Money.ofCents(e.getPriceCents()),
                e.isAvailable(), e.getPosition(), e.isActive());
    }

    static OptionItemJpaEntity toEntity(OptionItem i) {
        return new OptionItemJpaEntity(i.id().value(), i.establishmentId().value(), i.groupId().value(), i.name(),
                i.price().cents(), i.available(), i.position(), i.active());
    }

    static ProductVariant toDomain(ProductVariantJpaEntity e) {
        return new ProductVariant(new ProductVariantId(e.getId()), new EstablishmentId(e.getEstablishmentId()),
                new ProductId(e.getProductId()), e.getName(), Money.ofCents(e.getPriceCents()),
                e.getPromoPriceCents() == null ? null : Money.ofCents(e.getPromoPriceCents()), e.isAvailable(),
                e.isRodizioDefault(), e.getPosition(), e.isActive());
    }

    static ProductVariantJpaEntity toEntity(ProductVariant v) {
        return new ProductVariantJpaEntity(v.id().value(), v.establishmentId().value(), v.productId().value(),
                v.name(), v.price().cents(), v.promoPrice() == null ? null : v.promoPrice().cents(), v.available(),
                v.rodizioDefault(), v.position(), v.active());
    }
}
