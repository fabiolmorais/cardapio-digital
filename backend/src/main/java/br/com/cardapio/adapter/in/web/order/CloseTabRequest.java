package br.com.cardapio.adapter.in.web.order;

import br.com.cardapio.domain.call.model.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(name = "CloseTabRequest")
record CloseTabRequest(
        @Schema(description = "Como a mesa pagou o que faltava; ausente se ela já pagou tudo adiantado")
        PaymentMethod paymentMethod,
        @Schema(description = "A mesa pagou a taxa de serviço? Vazio: fica a escolha que já estava")
        Boolean serviceAccepted,
        @Schema(description = "Outro valor de serviço, em centavos, no lugar do percentual (se o restaurante permitir)",
                example = "1000")
        @Min(1) @Max(100_000) Long serviceCustomCents
) {
}
