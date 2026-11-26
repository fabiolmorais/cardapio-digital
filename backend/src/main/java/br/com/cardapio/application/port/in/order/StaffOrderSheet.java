package br.com.cardapio.application.port.in.order;

import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.order.model.AlreadyOrdered;
import br.com.cardapio.domain.order.model.CounterGuest;

import java.util.List;
import java.util.Set;

/**
 * O que o garçom precisa para lançar um pedido nesta mesa.
 *
 * @param orderingEnabled o restaurante liberou o lançamento pela equipe
 * @param alreadyOrdered  o que a mesa já pediu e ainda conta para a trava de duplicados
 * @param rodizio         o rodízio ativo na comanda; null se não houver
 */
public record StaffOrderSheet(DiningTableId tableId, String tableLabel, boolean orderingEnabled,
                              List<AlreadyOrdered> alreadyOrdered, Rodizio rodizio, CounterGuest counter) {

    /** Numa mesa: sem o cliente do balcão. */
    public StaffOrderSheet(DiningTableId tableId, String tableLabel, boolean orderingEnabled,
                           List<AlreadyOrdered> alreadyOrdered, Rodizio rodizio) {
        this(tableId, tableLabel, orderingEnabled, alreadyOrdered, rodizio, null);
    }

    public StaffOrderSheet {
        alreadyOrdered = List.copyOf(alreadyOrdered);
    }

    /** @param productIds os produtos incluídos, que podem entrar nas rodadas */
    public record Rodizio(String name, Set<ProductId> productIds) {

        public Rodizio {
            productIds = Set.copyOf(productIds);
        }
    }
}
