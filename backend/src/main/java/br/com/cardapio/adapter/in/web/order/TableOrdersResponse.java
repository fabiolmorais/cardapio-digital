package br.com.cardapio.adapter.in.web.order;

import br.com.cardapio.adapter.in.web.rodizio.TableRodizioResponse;
import br.com.cardapio.application.port.in.order.TableOrders;
import br.com.cardapio.domain.rodizio.service.RoundPolicy;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "TableOrders")
record TableOrdersResponse(
        @Schema(requiredMode = REQUIRED, description = "O restaurante aceita pedido pelo celular")
        boolean orderingEnabled,
        @Schema(requiredMode = REQUIRED, description = "Os itens não cancelados mais o rodízio (pessoas × preço)")
        long totalCents,
        @Schema(description = "O rodízio da mesa; ausente se ela não está no rodízio") TableRodizioResponse rodizio,
        @Schema(requiredMode = REQUIRED, description = "Os produtos incluídos no rodízio (a aba Rodízio)")
        List<UUID> rodizioProductIds,
        @Schema(description = "As regras do rodízio ligadas, em números para a tela; ausente fora do rodízio")
        RodizioLimits rodizioLimits,
        @Schema(requiredMode = REQUIRED, description = "Os pedidos da comanda aberta, do mais antigo ao mais novo")
        List<TableOrderResponse> orders
) {

    @Schema(name = "RodizioLimits", description = "Cada campo ausente: a regra está desligada")
    record RodizioLimits(
            @Schema(description = "Unidades de cada item numa rodada, para a mesa toda") Integer maxPerItem,
            @Schema(description = "Quantos itens cabem agora, pelo limite de itens não entregues") Integer room,
            @Schema(description = "Quando a próxima rodada libera (o intervalo entre rodadas)") Instant nextRoundAt,
            @Schema(description = "O fim do rodízio (o tempo limite)") Instant endsAt,
            @Schema(description = "O aviso de desperdício, para o topo da aba Rodízio") String wasteNotice
    ) {
        static RodizioLimits from(RoundPolicy.Limits l) {
            return l == null ? null : new RodizioLimits(l.maxPerItem(), l.room(), l.nextRoundAt(), l.endsAt(),
                    l.wasteNotice());
        }
    }

    static TableOrdersResponse from(TableOrders t) {
        return new TableOrdersResponse(t.orderingEnabled(), t.total().cents(),
                t.rodizio() == null ? null : TableRodizioResponse.from(t.rodizio()),
                t.rodizioProducts().stream().map(p -> p.value()).sorted().toList(),
                RodizioLimits.from(t.rodizioLimits()),
                t.orders().stream().map(TableOrderResponse::from).toList());
    }
}
