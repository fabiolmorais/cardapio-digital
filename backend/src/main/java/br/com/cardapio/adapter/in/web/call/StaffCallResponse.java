package br.com.cardapio.adapter.in.web.call;

import br.com.cardapio.application.port.in.call.StaffCall;
import br.com.cardapio.domain.call.model.CallStatus;
import br.com.cardapio.domain.call.model.CallType;
import br.com.cardapio.domain.call.model.PaymentMethod;
import br.com.cardapio.domain.tablesession.model.PresenceMethod;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "StaffCall")
record StaffCallResponse(
        @Schema(requiredMode = REQUIRED) UUID id,
        @Schema(requiredMode = REQUIRED) String tableLabel,
        @Schema(requiredMode = REQUIRED) String sectorName,
        @Schema(requiredMode = REQUIRED) CallType type,
        PaymentMethod paymentMethod,
        @Schema(requiredMode = REQUIRED) CallStatus status,
        @Schema(requiredMode = REQUIRED) Instant createdAt,
        Instant acceptedAt,
        String acceptedByName,
        @Schema(description = "Como a mesa provou a presença (Wi-Fi, GPS ou garçom); vazio no pedido de liberação")
        PresenceMethod presenceMethod
) {
    static StaffCallResponse from(StaffCall c) {
        return new StaffCallResponse(c.id().value(), c.tableLabel(), c.sectorName(), c.type(), c.paymentMethod(),
                c.status(), c.createdAt(), c.acceptedAt(), c.acceptedByName(), c.presenceMethod());
    }
}
