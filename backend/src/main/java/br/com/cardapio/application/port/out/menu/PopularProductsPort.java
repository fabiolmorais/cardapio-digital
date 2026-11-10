package br.com.cardapio.application.port.out.menu;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.ProductId;

import java.time.Instant;
import java.util.List;

public interface PopularProductsPort {

    /**
     * Os produtos mais pedidos desde `since`, pela quantidade somada, do mais pedido ao menos:
     * sem os itens cancelados nem os do rodízio (que não têm preço). No máximo `limit`.
     */
    List<ProductId> findMostOrdered(EstablishmentId establishmentId, Instant since, int limit);
}
