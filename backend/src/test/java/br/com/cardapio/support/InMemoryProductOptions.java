package br.com.cardapio.support;

import br.com.cardapio.application.port.out.menu.ProductOptionsChange;
import br.com.cardapio.application.port.out.menu.ProductOptionsPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.menu.model.ProductOptions;
import br.com.cardapio.domain.menu.model.ProductVariant;
import br.com.cardapio.domain.menu.model.ProductVariantId;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** As opções dos produtos em memória, para os testes dos casos de uso (ADR 0010). */
public class InMemoryProductOptions implements ProductOptionsPort {

    private final Map<ProductId, ProductOptions> byProduct = new HashMap<>();

    /** Deixa o produto com estes tamanhos e grupos. */
    public void put(ProductOptions options) {
        byProduct.put(options.productId(), options);
    }

    @Override
    public ProductOptions find(EstablishmentId establishmentId, ProductId productId) {
        return byProduct.getOrDefault(productId, ProductOptions.none(productId));
    }

    @Override
    public Map<ProductId, ProductOptions> findFor(EstablishmentId establishmentId, Collection<ProductId> productIds) {
        Map<ProductId, ProductOptions> found = new HashMap<>();
        productIds.stream().filter(byProduct::containsKey).forEach(id -> found.put(id, byProduct.get(id)));
        return found;
    }

    @Override
    public Map<ProductId, ProductOptions> findAllActive(EstablishmentId establishmentId) {
        return Map.copyOf(byProduct);
    }

    @Override
    public List<ProductVariant> findVariants(EstablishmentId establishmentId, ProductId productId) {
        return find(establishmentId, productId).variants();
    }

    @Override
    public void save(ProductOptionsChange change) {
        throw new UnsupportedOperationException("use put()");
    }

    @Override
    public Optional<ProductVariant> findVariant(EstablishmentId establishmentId, ProductVariantId id) {
        return byProduct.values().stream().flatMap(o -> o.variants().stream()).filter(v -> v.id().equals(id))
                .findFirst();
    }

    @Override
    public void saveVariant(ProductVariant variant) {
        throw new UnsupportedOperationException("use put()");
    }
}
