package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.shared.model.Money;

import java.util.Objects;

/**
 * O que os celulares fizeram com um produto no período (REL-27, 28, 29), contando sessões da mesa: cada
 * celular conta uma vez por produto. "Pediu" é a mesma sessão ter pedido o produto.
 *
 * @param price          o preço de hoje do produto (a base do valor que ficou no carrinho)
 * @param viewers        as sessões que viram o produto (abriram a ficha ou viram o reel 2 s)
 * @param viewersOrdered dessas, as que pediram o produto
 * @param reelViewers    as sessões que viram o reel do produto
 * @param reelOrdered    dessas, as que pediram o produto
 * @param cartSessions   as sessões que puseram o produto no carrinho
 * @param leftSessions   dessas, as que não pediram o produto
 * @param leftUnits      as unidades que essas sessões puseram no carrinho e não pediram
 */
public record ProductInterestRow(String name, Money price, int viewers, int viewersOrdered, int reelViewers,
                                 int reelOrdered, int cartSessions, int leftSessions, int leftUnits) {

    public ProductInterestRow {
        Objects.requireNonNull(name);
        Objects.requireNonNull(price);
    }
}
