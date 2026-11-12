package br.com.cardapio.adapter.in.web.order;

import br.com.cardapio.domain.call.model.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CloseTabRequest")
record CloseTabRequest(
        @Schema(description = "Como a mesa pagou o que faltava; ausente se ela já pagou tudo adiantado")
        PaymentMethod paymentMethod
) {
}
