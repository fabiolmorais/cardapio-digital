package br.com.cardapio.adapter.in.web.order;

import br.com.cardapio.domain.order.model.TicketKind;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "ClaimPrintJobRequest")
record ClaimPrintJobRequest(
        @Schema(requiredMode = REQUIRED, description = "O pedido da comanda") @NotNull UUID orderId,
        @Schema(requiredMode = REQUIRED, description = "A comanda do pedido (ORDER) ou a via \"CANCELADO\" (CANCELLATION)")
        @NotNull TicketKind kind
) {
}
