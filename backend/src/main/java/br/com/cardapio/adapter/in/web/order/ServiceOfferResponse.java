package br.com.cardapio.adapter.in.web.order;

import br.com.cardapio.application.port.in.order.ServiceOffer;
import br.com.cardapio.domain.order.model.ServiceDestination;
import io.swagger.v3.oas.annotations.media.Schema;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "ServiceOffer", description = "A taxa de serviço opcional (os 10% do garçom): já calculada, com e sem")
public record ServiceOfferResponse(
        @Schema(requiredMode = REQUIRED, description = "O percentual", example = "10") int percent,
        @Schema(requiredMode = REQUIRED, description = "Está aceita (vem aceita; a mesa ou o garçom pode tirar)")
        boolean accepted,
        @Schema(requiredMode = REQUIRED, description = "A taxa escolhida (o outro valor, ou o percentual), em centavos")
        long amountCents,
        @Schema(requiredMode = REQUIRED, description = "O percentual sobre o consumo, em centavos")
        long percentAmountCents,
        @Schema(description = "O outro valor escolhido, em centavos; ausente quando vale o percentual")
        Long customCents,
        @Schema(requiredMode = REQUIRED, description = "O restaurante deixa escolher outro valor")
        boolean customAllowed,
        @Schema(requiredMode = REQUIRED, description = "O que falta pagar, com a taxa") long dueWithCents,
        @Schema(requiredMode = REQUIRED, description = "O que falta pagar, sem a taxa") long dueWithoutCents,
        @Schema(requiredMode = REQUIRED, description = "Para quem fica registrada") ServiceDestination destination,
        @Schema(description = "O garçom que fica com ela, se a regra for \"garçom que atendeu\"") String waiterName
) {

    public static ServiceOfferResponse from(ServiceOffer s) {
        return s == null ? null : new ServiceOfferResponse(s.percent(), s.accepted(), s.amount().cents(),
                s.percentAmount().cents(), s.custom() == null ? null : s.custom().cents(), s.customAllowed(),
                s.dueWith().cents(), s.dueWithout().cents(), s.destination(), s.waiterName());
    }
}
