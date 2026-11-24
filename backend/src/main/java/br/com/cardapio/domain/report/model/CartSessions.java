package br.com.cardapio.domain.report.model;

/**
 * Os carrinhos do período: as sessões que puseram algo no carrinho e, dessas, as que não enviaram
 * nenhum pedido.
 */
public record CartSessions(int withCart, int notSent) {
}
