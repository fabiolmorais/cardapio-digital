package br.com.cardapio.adapter.in.web.call;

import br.com.cardapio.application.port.in.order.BillSummary;
import br.com.cardapio.domain.call.model.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "Bill", description = "A conta que a mesa pediu: o consumo, o que já foi pago e o que falta")
record BillResponse(
        @Schema(description = "A comanda aberta da mesa; ausente se ela ainda não pediu nada") UUID tabId,
        @Schema(requiredMode = REQUIRED, description = "O consumo: itens não cancelados mais o rodízio")
        long totalCents,
        @Schema(requiredMode = REQUIRED, description = "O que a mesa já pagou adiantado") long paidCents,
        @Schema(requiredMode = REQUIRED, description = "O que falta receber") long dueCents,
        @Schema(requiredMode = REQUIRED, description = "A forma que a mesa escolheu ao pedir a conta")
        PaymentMethod requestedWith,
        @Schema(requiredMode = REQUIRED, description = "Quantos itens ainda não chegaram à mesa")
        int itemsOnTheWay
) {

    static BillResponse from(BillSummary b) {
        return new BillResponse(b.tabId() == null ? null : b.tabId().value(), b.total().cents(), b.paid().cents(),
                b.due().cents(), b.requestedWith(), b.itemsOnTheWay());
    }
}
