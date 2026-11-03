package br.com.cardapio.adapter.out.persistence.order;

/** Um pedido parado na confirmação, com a mesa e o setor (os itens vêm numa segunda consulta). */
public record WaitingOrderRow(OrderJpaEntity order, String tableLabel, String sectorName) {
}
