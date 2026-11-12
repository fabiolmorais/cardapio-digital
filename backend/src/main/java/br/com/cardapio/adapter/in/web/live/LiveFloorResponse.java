package br.com.cardapio.adapter.in.web.live;

import br.com.cardapio.application.port.in.live.LiveFloor;
import br.com.cardapio.domain.call.model.CallStatus;
import br.com.cardapio.domain.call.model.CallType;
import br.com.cardapio.domain.call.model.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "LiveFloor", description = "O salão agora: cada mesa ativa e o resumo do dia")
record LiveFloorResponse(
        @Schema(requiredMode = REQUIRED, description = "As mesas ativas, por setor e número") List<Table> tables,
        @Schema(requiredMode = REQUIRED, description = "Mesas com gente, comanda ou chamado") int occupied,
        @Schema(description = "O faturado de hoje (comandas fechadas); ausente para quem não é dono ou gerente")
        Long revenueTodayCents,
        @Schema(description = "A espera média dos chamados de hoje, em segundos; ausente se ninguém chamou")
        Long averageWaitSeconds
) {

    @Schema(name = "LiveTable")
    record Table(
            @Schema(requiredMode = REQUIRED) UUID id,
            @Schema(requiredMode = REQUIRED) String label,
            @Schema(requiredMode = REQUIRED) UUID sectorId,
            @Schema(requiredMode = REQUIRED) String sectorName,
            @Schema(requiredMode = REQUIRED, description = "O que pinta a mesa no mapa") LiveFloor.State state,
            @Schema(requiredMode = REQUIRED, description = "Celulares com a sessão ativa") int phones,
            @Schema(description = "A comanda aberta; ausente se a mesa não tem") UUID tabId,
            @Schema(description = "Desde quando a comanda está aberta") Instant tabOpenedAt,
            @Schema(requiredMode = REQUIRED, description = "Itens não cancelados mais o rodízio") long tabTotalCents,
            @Schema(requiredMode = REQUIRED) int itemsOnTheWay,
            @Schema(requiredMode = REQUIRED, description = "Pessoas no rodízio; zero fora do rodízio") int rodizioPeople,
            @Schema(description = "O chamado aberto que mais importa; ausente se nenhum") Call call,
            @Schema(requiredMode = REQUIRED, description = "Pedidos esperando a confirmação do garçom") int waitingOrders
    ) {
    }

    @Schema(name = "LiveTableCall")
    record Call(
            @Schema(requiredMode = REQUIRED) CallType type,
            @Schema(requiredMode = REQUIRED) CallStatus status,
            PaymentMethod paymentMethod,
            @Schema(requiredMode = REQUIRED) Instant createdAt,
            String acceptedByName
    ) {
    }

    static LiveFloorResponse from(LiveFloor f) {
        return new LiveFloorResponse(f.tables().stream().map(t -> new Table(t.id().value(), t.label(),
                        t.sectorId().value(), t.sectorName(), t.state(), t.phones(),
                        t.tabId() == null ? null : t.tabId().value(), t.tabOpenedAt(), t.tabTotal().cents(),
                        t.itemsOnTheWay(), t.rodizioPeople(),
                        t.call() == null ? null : new Call(t.call().type(), t.call().status(), t.call().paymentMethod(),
                                t.call().createdAt(), t.call().acceptedByName()),
                        t.waitingOrders())).toList(),
                f.occupied(), f.revenueToday() == null ? null : f.revenueToday().cents(), f.averageWaitSeconds());
    }
}
