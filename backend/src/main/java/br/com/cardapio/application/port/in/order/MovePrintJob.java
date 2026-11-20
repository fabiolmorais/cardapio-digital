package br.com.cardapio.application.port.in.order;

import br.com.cardapio.application.port.out.order.MovePrintView;
import br.com.cardapio.domain.order.model.TabMoveId;

import java.time.Instant;
import java.util.List;

/**
 * A via "MUDOU DE MESA" (ou "JUNTOU") para a impressora da cozinha: de qual mesa para qual, e os
 * itens a caminho que tinham saído no papel com a mesa antiga (ADR 0012).
 */
public record MovePrintJob(TabMoveId moveId, boolean merged, String fromTableLabel, String toTableLabel,
                           String sectorName, Instant movedAt, List<PrintJob.Line> items) {

    public MovePrintJob {
        items = List.copyOf(items);
    }

    public static MovePrintJob from(MovePrintView view) {
        return new MovePrintJob(view.moveId(), view.merged(), view.fromTableLabel(), view.toTableLabel(),
                view.sectorName(), view.movedAt(), view.items().stream()
                .map(i -> new PrintJob.Line(i.id(), i.productName(), i.quantity(), i.note(), i.inRodizio(),
                        null, i.choices()))
                .toList());
    }
}
