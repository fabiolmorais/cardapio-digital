package br.com.cardapio.adapter.out.persistence.menu;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentFilter;
import br.com.cardapio.application.port.out.menu.ProductOptionsChange;
import br.com.cardapio.application.port.out.menu.ProductOptionsPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.ChoiceLimit;
import br.com.cardapio.domain.menu.model.OptionGroup;
import br.com.cardapio.domain.menu.model.OptionItem;
import br.com.cardapio.domain.menu.model.OptionItemId;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.menu.model.ProductOptions;
import br.com.cardapio.domain.menu.model.ProductVariant;
import br.com.cardapio.domain.menu.model.ProductVariantId;
import br.com.cardapio.domain.shared.model.Money;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Os tamanhos e os grupos de cada produto. Os tamanhos são gravados um a um (nunca apagados: os
 * pedidos apontam para eles); as ligações, os limites e os preços por tamanho são trocados inteiros,
 * como o rodízio faz com as faixas. Ler, para um produto ou para o cardápio todo, são sempre seis
 * consultas (sem N+1).
 */
@Component
class ProductOptionsPersistenceAdapter implements ProductOptionsPort {

    private final ProductVariantJpaRepository variants;
    private final ProductOptionGroupJpaRepository links;
    private final ProductOptionGroupLimitJpaRepository limits;
    private final OptionItemPriceJpaRepository prices;
    private final OptionGroupJpaRepository groups;
    private final OptionItemJpaRepository items;
    private final EstablishmentFilter establishmentFilter;

    ProductOptionsPersistenceAdapter(ProductVariantJpaRepository variants, ProductOptionGroupJpaRepository links,
                                     ProductOptionGroupLimitJpaRepository limits, OptionItemPriceJpaRepository prices,
                                     OptionGroupJpaRepository groups, OptionItemJpaRepository items,
                                     EstablishmentFilter establishmentFilter) {
        this.variants = variants;
        this.links = links;
        this.limits = limits;
        this.prices = prices;
        this.groups = groups;
        this.items = items;
        this.establishmentFilter = establishmentFilter;
    }

    @Override
    public ProductOptions find(EstablishmentId establishmentId, ProductId productId) {
        return findFor(establishmentId, List.of(productId)).getOrDefault(productId, ProductOptions.none(productId));
    }

    @Override
    public Map<ProductId, ProductOptions> findFor(EstablishmentId establishmentId, Collection<ProductId> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        establishmentFilter.enable(establishmentId);
        UUID establishment = establishmentId.value();
        List<UUID> ids = productIds.stream().map(ProductId::value).toList();
        return assemble(establishmentId,
                variants.findByEstablishmentIdAndProductIdInAndActiveTrueOrderByPositionAsc(establishment, ids),
                links.findByEstablishmentIdAndProductIdInOrderByPositionAsc(establishment, ids),
                limits.findByEstablishmentIdAndProductIdIn(establishment, ids),
                prices.findByEstablishmentIdAndProductIdIn(establishment, ids));
    }

    @Override
    public Map<ProductId, ProductOptions> findAllActive(EstablishmentId establishmentId) {
        establishmentFilter.enable(establishmentId);
        UUID establishment = establishmentId.value();
        return assemble(establishmentId,
                variants.findByEstablishmentIdAndActiveTrueOrderByPositionAsc(establishment),
                links.findByEstablishmentIdOrderByPositionAsc(establishment),
                limits.findByEstablishmentId(establishment),
                prices.findByEstablishmentId(establishment));
    }

    @Override
    public List<ProductVariant> findVariants(EstablishmentId establishmentId, ProductId productId) {
        establishmentFilter.enable(establishmentId);
        return variants.findByEstablishmentIdAndProductIdOrderByPositionAsc(establishmentId.value(), productId.value())
                .stream().map(OptionMapper::toDomain).toList();
    }

    @Override
    public void save(ProductOptionsChange change) {
        establishmentFilter.enable(change.establishmentId());
        UUID establishment = change.establishmentId().value();
        UUID product = change.productId().value();
        variants.saveAll(change.variants().stream().map(OptionMapper::toEntity).toList());
        variants.flush();
        prices.deleteByProduct(establishment, product);
        limits.deleteByProduct(establishment, product);
        links.deleteByProduct(establishment, product);
        List<ProductOptionGroupJpaEntity> newLinks = new ArrayList<>();
        for (int position = 0; position < change.groups().size(); position++) {
            newLinks.add(new ProductOptionGroupJpaEntity(UUID.randomUUID(), establishment, product,
                    change.groups().get(position).value(), position));
        }
        links.saveAll(newLinks);
        limits.saveAll(change.limits().stream().map(l -> new ProductOptionGroupLimitJpaEntity(UUID.randomUUID(),
                establishment, product, l.groupId().value(), l.variantId().value(), l.limit().minChoices(),
                l.limit().maxChoices())).toList());
        prices.saveAll(change.prices().stream().map(p -> new OptionItemPriceJpaEntity(UUID.randomUUID(),
                establishment, product, p.optionId().value(), p.variantId().value(), p.price().cents())).toList());
    }

    @Override
    public Optional<ProductVariant> findVariant(EstablishmentId establishmentId, ProductVariantId id) {
        establishmentFilter.enable(establishmentId);
        return variants.findByIdAndEstablishmentId(id.value(), establishmentId.value()).map(OptionMapper::toDomain);
    }

    @Override
    public void saveVariant(ProductVariant variant) {
        establishmentFilter.enable(variant.establishmentId());
        variants.save(OptionMapper.toEntity(variant));
    }

    /** Junta em memória o que veio do banco, um ProductOptions por produto que tem tamanho ou grupo. */
    private Map<ProductId, ProductOptions> assemble(EstablishmentId establishmentId,
                                                    List<ProductVariantJpaEntity> foundVariants,
                                                    List<ProductOptionGroupJpaEntity> foundLinks,
                                                    List<ProductOptionGroupLimitJpaEntity> foundLimits,
                                                    List<OptionItemPriceJpaEntity> foundPrices) {
        if (foundVariants.isEmpty() && foundLinks.isEmpty()) {
            return Map.of();
        }
        Set<UUID> groupIds = foundLinks.stream().map(ProductOptionGroupJpaEntity::getOptionGroupId)
                .collect(Collectors.toSet());
        Map<UUID, OptionGroup> activeGroups = groupIds.isEmpty() ? Map.of()
                : groups.findByEstablishmentIdAndActiveTrueAndIdIn(establishmentId.value(), groupIds).stream()
                .collect(Collectors.toMap(OptionGroupJpaEntity::getId, OptionMapper::toDomain));
        Map<UUID, List<OptionItem>> itemsByGroup = activeGroups.isEmpty() ? Map.of()
                : items.findByEstablishmentIdAndOptionGroupIdInAndActiveTrueOrderByPositionAsc(
                        establishmentId.value(), activeGroups.keySet()).stream().map(OptionMapper::toDomain)
                .collect(Collectors.groupingBy(i -> i.groupId().value()));

        Map<UUID, List<ProductVariant>> variantsByProduct = foundVariants.stream().map(OptionMapper::toDomain)
                .collect(Collectors.groupingBy(v -> v.productId().value()));
        Map<UUID, List<ProductOptionGroupJpaEntity>> linksByProduct = foundLinks.stream()
                .collect(Collectors.groupingBy(ProductOptionGroupJpaEntity::getProductId));
        Map<String, Map<ProductVariantId, ChoiceLimit>> limitsByProductGroup = new HashMap<>();
        for (ProductOptionGroupLimitJpaEntity l : foundLimits) {
            limitsByProductGroup.computeIfAbsent(l.getProductId() + "/" + l.getOptionGroupId(), k -> new HashMap<>())
                    .put(new ProductVariantId(l.getProductVariantId()),
                            new ChoiceLimit(l.getMinChoices(), l.getMaxChoices()));
        }
        Map<UUID, Map<ProductOptions.PriceKey, Money>> pricesByProduct = new HashMap<>();
        for (OptionItemPriceJpaEntity p : foundPrices) {
            pricesByProduct.computeIfAbsent(p.getProductId(), k -> new HashMap<>())
                    .put(new ProductOptions.PriceKey(new OptionItemId(p.getOptionItemId()),
                            new ProductVariantId(p.getProductVariantId())), Money.ofCents(p.getPriceCents()));
        }

        Set<UUID> products = new HashSet<>(variantsByProduct.keySet());
        products.addAll(linksByProduct.keySet());
        Map<ProductId, ProductOptions> result = new LinkedHashMap<>();
        for (UUID product : products) {
            List<ProductOptions.LinkedGroup> linked = linksByProduct.getOrDefault(product, List.of()).stream()
                    .filter(l -> activeGroups.containsKey(l.getOptionGroupId()))
                    .map(l -> new ProductOptions.LinkedGroup(activeGroups.get(l.getOptionGroupId()),
                            itemsByGroup.getOrDefault(l.getOptionGroupId(), List.of()),
                            limitsByProductGroup.getOrDefault(product + "/" + l.getOptionGroupId(), Map.of())))
                    .toList();
            ProductOptions options = new ProductOptions(new ProductId(product),
                    variantsByProduct.getOrDefault(product, List.of()), linked,
                    pricesByProduct.getOrDefault(product, Map.of()));
            if (!options.isEmpty()) {
                result.put(options.productId(), options);
            }
        }
        return result;
    }

}
