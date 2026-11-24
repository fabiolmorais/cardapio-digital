package br.com.cardapio.domain.report.model;

/** Um produto que ficou no carrinho: em quantas sessões e quantas unidades. */
public record LeftInCart(String name, int sessions, int units) {
}
