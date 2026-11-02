package br.com.cardapio.adapter.in.web.call;

import br.com.cardapio.domain.call.model.CallType;
import br.com.cardapio.domain.call.model.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/** Um toque num botão da mesa. A forma de pagamento só vale (e é obrigatória) no pedido de conta. */
@Schema(name = "CallRequest")
record CallRequest(
        @Schema(requiredMode = REQUIRED) @NotNull CallType type,
        @Schema(description = "Só no pedido de conta (BILL)") PaymentMethod paymentMethod
) {
}
