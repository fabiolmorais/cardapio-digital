package br.com.cardapio.application.port.out.menu;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.menu.model.ProductOptions;
import br.com.cardapio.domain.menu.model.ProductVariant;
import br.com.cardapio.domain.menu.model.ProductVariantId;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Os tamanhos e os grupos ligados a cada produto (ADR 0010). */
public interface ProductOptionsPort {

    /** A configuração do produto; {@link ProductOptions#none} se ele não tem tamanhos nem grupos. */
    ProductOptions find(EstablishmentId establishmentId, ProductId productId);

    /** A configuração de vários produtos de uma vez (o pedido); quem não tem opções fica de fora do mapa. */
    Map<ProductId, ProductOptions> findFor(EstablishmentId establishmentId, Collection<ProductId> productIds);

    /** A configuração de todos os produtos ativos (o cardápio da mesa); quem não tem opções fica de fora. */
    Map<ProductId, ProductOptions> findAllActive(EstablishmentId establishmentId);

    /** Todos os tamanhos do produto, inclusive os desativados (para a gestão reaproveitar os ids). */
    List<ProductVariant> findVariants(EstablishmentId establishmentId, ProductId productId);

    void save(ProductOptionsChange change);

    Optional<ProductVariant> findVariant(EstablishmentId establishmentId, ProductVariantId id);

    void saveVariant(ProductVariant variant);
}
