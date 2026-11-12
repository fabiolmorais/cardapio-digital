package br.com.cardapio.adapter.in.web.call;

import br.com.cardapio.domain.call.model.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "ReceivePaymentRequest", description = "O que o garçom recebeu da mesa ao atender o pedido de conta")
record ReceivePaymentRequest(
        @Schema(requiredMode = REQUIRED, description = "Valor recebido, em centavos (até o que falta pagar)",
                example = "8790")
        @NotNull @Min(1) @Max(100_000_000) Long amountCents,
        @Schema(requiredMode = REQUIRED, description = "Como a mesa pagou") @NotNull PaymentMethod paymentMethod
) {
}
