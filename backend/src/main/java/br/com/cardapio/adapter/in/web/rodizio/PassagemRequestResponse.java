package br.com.cardapio.adapter.in.web.rodizio;

import br.com.cardapio.application.port.in.order.KitchenItem;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "PassagemRequest", description = "Um corte pedido pela mesa, para o passador levar")
record PassagemRequestResponse(
        @Schema(requiredMode = REQUIRED, description = "O item do pedido (o passador entrega ou cancela por ele)")
        UUID id,
        @Schema(requiredMode = REQUIRED, example = "04") String tableLabel,
        @Schema(requiredMode = REQUIRED) String sectorName,
        @Schema(requiredMode = REQUIRED, example = "Picanha") String productName,
        @Schema(requiredMode = REQUIRED) int quantity,
        @Schema(description = "O ponto, ou outra observação da mesa", example = "Mal passada") String note,
        @Schema(requiredMode = REQUIRED, description = "Quando foi pedido") Instant createdAt
) {

    static PassagemRequestResponse from(KitchenItem i) {
        return new PassagemRequestResponse(i.id().value(), i.tableLabel(), i.sectorName(), i.productName(),
                i.quantity(), i.note(), i.createdAt());
    }
}
