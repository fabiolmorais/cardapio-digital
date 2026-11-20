package br.com.cardapio.application.port.out.order;

import br.com.cardapio.domain.order.model.OrderItem;
import br.com.cardapio.domain.order.model.TabMoveId;

import java.time.Instant;
import java.util.List;

/**
 * A via "MUDOU DE MESA" (ou "JUNTOU"): os itens que já tinham saído na impressora com a mesa antiga
 * e ainda estão a caminho (ADR 0012).
 *
 * @param sectorName o setor da mesa nova
 * @param items      os itens a caminho, como estão agora (os já entregues ou cancelados não vêm)
 */
public record MovePrintView(TabMoveId moveId, boolean merged, String fromTableLabel, String toTableLabel,
                            String sectorName, Instant movedAt, List<OrderItem> items) {

    public MovePrintView {
        items = List.copyOf(items);
    }
}
