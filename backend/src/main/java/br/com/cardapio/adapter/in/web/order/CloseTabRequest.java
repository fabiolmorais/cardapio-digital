package br.com.cardapio.adapter.in.web.order;

import br.com.cardapio.domain.call.model.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "CloseTabRequest")
record CloseTabRequest(
        @Schema(requiredMode = REQUIRED, description = "Como a mesa pagou") @NotNull PaymentMethod paymentMethod
) {
}
