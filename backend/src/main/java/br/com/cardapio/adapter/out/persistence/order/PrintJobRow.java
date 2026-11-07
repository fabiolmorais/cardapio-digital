package br.com.cardapio.adapter.out.persistence.order;

import java.time.Instant;
import java.util.UUID;

/** O cabeçalho de uma comanda: o pedido, a mesa e o setor (os itens vêm numa segunda consulta, com IN). */
public record PrintJobRow(UUID orderId, int orderNumber, UUID tableId, String tableLabel, String sectorName,
                          Instant sortAt) {
}
