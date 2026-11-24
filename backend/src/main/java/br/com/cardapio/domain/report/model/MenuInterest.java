package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.shared.model.Money;

import java.time.Instant;
import java.util.List;

/**
 * Os eventos do celular no Cardápio (Business, REL-27, 28, 29).
 *
 * @param recordingSince o primeiro evento gravado do restaurante; null se nenhum celular gravou ainda
 * @param viewed         visto x pedido, dos mais vistos
 * @param reels          os reels mais vistos e quantos viraram pedido
 * @param carts          os carrinhos do período
 * @param leftValue      o que ficou no carrinho sem ser pedido, pelo preço de hoje
 * @param mostLeft       os produtos que mais ficam no carrinho
 */
public record MenuInterest(Instant recordingSince, List<ProductInterest> viewed, List<ProductInterest> reels,
                           CartSessions carts, Money leftValue, List<LeftInCart> mostLeft) {
}
