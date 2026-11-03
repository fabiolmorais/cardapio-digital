package br.com.cardapio.application.port.out.order;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.order.model.Tab;

import java.util.Optional;

public interface TabPort {

    /** A comanda aberta da mesa, se houver (no máximo uma: o banco garante). */
    Optional<Tab> findOpen(EstablishmentId establishmentId, DiningTableId tableId);

    /**
     * Abre a comanda, a menos que a mesa já tenha uma aberta (dois celulares mandando o primeiro
     * pedido no mesmo instante). false: já havia uma, e nada foi gravado.
     */
    boolean insertIfNoneOpen(Tab tab);
}
