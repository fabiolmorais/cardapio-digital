package br.com.cardapio.adapter.out.persistence.order;

import java.time.Instant;
import java.util.UUID;

/** As projeções das consultas nativas das mudanças de mesa. */
final class TabMoveRows {

    private TabMoveRows() {
    }

    /** Uma mudança no detalhe da comanda. */
    interface History {
        String getFromTableLabel();

        String getToTableLabel();

        boolean getMerged();

        String getMovedByName();

        Instant getMovedAt();
    }

    /** Um item de uma via "MUDOU DE MESA". */
    interface MoveItem {
        UUID getMoveId();

        UUID getItemId();
    }
}
