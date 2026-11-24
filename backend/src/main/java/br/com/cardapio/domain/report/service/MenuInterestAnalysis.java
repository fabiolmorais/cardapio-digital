package br.com.cardapio.domain.report.service;

import br.com.cardapio.domain.report.model.CartSessions;
import br.com.cardapio.domain.report.model.LeftInCart;
import br.com.cardapio.domain.report.model.MenuInterest;
import br.com.cardapio.domain.report.model.ProductInterest;
import br.com.cardapio.domain.report.model.ProductInterestRow;
import br.com.cardapio.domain.shared.model.Money;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * As regras dos eventos do celular no Cardápio (REL-27, 28, 29): quem viu e pediu, os reels e os
 * carrinhos. "Visto" é abrir a ficha do prato ou ver o reel 2 s; "pediu" é a mesma sessão ter pedido o
 * produto.
 */
public final class MenuInterestAnalysis {

    /** Quantos produtos em "Visto x pedido". */
    public static final int TOP_VIEWED = 10;

    /** Quantos em "Reels mais vistos" e em "Ficam no carrinho". */
    public static final int TOP_REELS = 5;
    public static final int TOP_LEFT = 5;

    private MenuInterestAnalysis() {
    }

    public static MenuInterest of(Instant recordingSince, List<ProductInterestRow> rows, CartSessions carts) {
        List<ProductInterest> viewed = rows.stream()
                .filter(r -> r.viewers() > 0)
                .map(r -> new ProductInterest(r.name(), r.viewers(), r.viewersOrdered()))
                .sorted(Comparator.comparingInt(ProductInterest::viewers).reversed()
                        .thenComparing(ProductInterest::name))
                .limit(TOP_VIEWED)
                .toList();
        List<ProductInterest> reels = rows.stream()
                .filter(r -> r.reelViewers() > 0)
                .map(r -> new ProductInterest(r.name(), r.reelViewers(), r.reelOrdered()))
                .sorted(Comparator.comparingInt(ProductInterest::viewers).reversed()
                        .thenComparing(ProductInterest::name))
                .limit(TOP_REELS)
                .toList();
        Money left = rows.stream()
                .map(r -> r.price().times(r.leftUnits()))
                .reduce(Money.ZERO, Money::plus);
        List<LeftInCart> mostLeft = rows.stream()
                .filter(r -> r.leftSessions() > 0)
                .map(r -> new LeftInCart(r.name(), r.leftSessions(), r.leftUnits()))
                .sorted(Comparator.comparingInt(LeftInCart::sessions).reversed()
                        .thenComparing(Comparator.comparingInt(LeftInCart::units).reversed())
                        .thenComparing(LeftInCart::name))
                .limit(TOP_LEFT)
                .toList();
        return new MenuInterest(recordingSince, viewed, reels, carts, left, mostLeft);
    }
}
