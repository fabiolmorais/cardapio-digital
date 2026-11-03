package br.com.cardapio.adapter.out.persistence.order;

import java.util.UUID;

/** Uma linha da fila da cozinha: o item, o número do pedido, a mesa e o setor, numa consulta só. */
public record KitchenItemRow(OrderItemJpaEntity item, int orderNumber, UUID tableId, String tableLabel,
                             String sectorName) {
}
