package br.com.cardapio.adapter.in.web.order;

import br.com.cardapio.application.port.in.order.PrintJob;
import br.com.cardapio.domain.order.model.TicketKind;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "PrintJob", description = "Uma comanda para a impressora da cozinha")
record PrintJobResponse(
        @Schema(requiredMode = REQUIRED) UUID orderId,
        @Schema(requiredMode = REQUIRED) TicketKind kind,
        @Schema(requiredMode = REQUIRED, description = "O número do pedido no dia") int orderNumber,
        @Schema(requiredMode = REQUIRED) String tableLabel,
        @Schema(requiredMode = REQUIRED) String sectorName,
        @Schema(description = "Quando o pedido chegou; vazio se não há itens") Instant orderedAt,
        @Schema(requiredMode = REQUIRED, description = "Os itens desta via; vazio se outra estação já imprimiu")
        List<Line> items
) {

    @Schema(name = "PrintJobLine")
    record Line(
            @Schema(requiredMode = REQUIRED) UUID id,
            @Schema(requiredMode = REQUIRED) String productName,
            @Schema(requiredMode = REQUIRED) int quantity,
            String note,
            @Schema(requiredMode = REQUIRED, description = "Veio numa rodada do rodízio") boolean rodizio,
            @Schema(description = "O motivo, na via \"CANCELADO\"") String cancelReason,
            @Schema(requiredMode = REQUIRED, description = "O tamanho e as escolhas, embaixo do item")
            ChoicesResponse choices
    ) {
    }

    static PrintJobResponse from(PrintJob job) {
        return new PrintJobResponse(job.orderId().value(), job.kind(), job.orderNumber(), job.tableLabel(),
                job.sectorName(), job.orderedAt(), job.items().stream()
                .map(l -> new Line(l.id().value(), l.productName(), l.quantity(), l.note(), l.rodizio(),
                        l.cancelReason(), ChoicesResponse.from(l.choices())))
                .toList());
    }
}
