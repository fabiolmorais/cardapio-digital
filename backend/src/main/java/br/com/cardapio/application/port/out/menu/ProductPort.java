package br.com.cardapio.application.port.out.menu;

import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.MenuCategoryId;
import br.com.cardapio.domain.menu.model.Product;
import br.com.cardapio.domain.menu.model.ProductId;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductPort {

    void save(Product product);

    /** Vazio se o produto não existe ou é de outro restaurante. */
    Optional<Product> find(EstablishmentId establishmentId, ProductId id);

    /**
     * Produtos ativos na ordem do cardápio (a das categorias, depois a de dentro de cada uma).
     *
     * @param categoryId só desta categoria; null para todas
     * @param text       só os que têm este texto no nome, sem diferenciar maiúsculas; null para todos
     */
    ResultPage<Product> search(EstablishmentId establishmentId, MenuCategoryId categoryId, String text,
                               PageQuery page);

    /** Todos os produtos ativos da categoria, na ordem (para reordenar). */
    List<Product> findActiveByCategory(EstablishmentId establishmentId, MenuCategoryId categoryId);

    /** Posição para um produto que chega na categoria: depois de todos os que existem nela. */
    int nextPosition(EstablishmentId establishmentId, MenuCategoryId categoryId);

    /** Quantos produtos ativos o restaurante tem (o teto é Product.MAX_PER_ESTABLISHMENT). */
    long countActive(EstablishmentId establishmentId);

    /**
     * Todos os produtos ativos, na ordem do cardápio, numa consulta só: é o que a página da mesa
     * mostra. São no máximo Product.MAX_PER_ESTABLISHMENT (ADR 0006).
     */
    List<Product> findAllActive(EstablishmentId establishmentId);

    /** Os produtos ativos destes ids, numa consulta só (o carrinho). Os que não existem ou são de outro restaurante ficam de fora. */
    List<Product> findActiveByIds(EstablishmentId establishmentId, Collection<ProductId> ids);
}
