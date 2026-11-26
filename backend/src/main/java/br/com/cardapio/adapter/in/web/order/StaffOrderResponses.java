package br.com.cardapio.adapter.in.web.order;

import br.com.cardapio.application.port.in.order.StaffOrder;
import br.com.cardapio.application.port.in.order.StaffOrderSheet;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.order.model.AlreadyOrdered;
import br.com.cardapio.domain.order.model.OrderItemStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/** O que a API devolve no lançamento de pedidos pela equipe (ADR 0011). */
final class StaffOrderResponses {

    private StaffOrderResponses() {
    }

    @Schema(name = "StaffOrder")
    record Placed(
            @Schema(requiredMode = REQUIRED) UUID id,
            @Schema(requiredMode = REQUIRED, description = "O número do pedido no dia") int number,
            @Schema(requiredMode = REQUIRED) UUID tabId
    ) {
        static Placed from(StaffOrder o) {
            return new Placed(o.id().value(), o.number(), o.tabId().value());
        }
    }

    @Schema(name = "AlreadyOrderedItem", description = "Um item que a mesa acabou de pedir ou que ainda não chegou")
    record Item(
            @Schema(requiredMode = REQUIRED) UUID productId,
            @Schema(requiredMode = REQUIRED) String productName,
            @Schema(requiredMode = REQUIRED) int quantity,
            @Schema(requiredMode = REQUIRED) ChoicesResponse choices,
            @Schema(requiredMode = REQUIRED) OrderItemStatus status,
            @Schema(requiredMode = REQUIRED) Instant orderedAt,
            @Schema(requiredMode = REQUIRED, description = "Foi a equipe que lançou (senão, um celular da mesa)")
            boolean byStaff,
            @Schema(description = "Quem da equipe lançou") String placedByName
    ) {
        static Item from(AlreadyOrdered i) {
            return new Item(i.productId().value(), i.productName(), i.quantity(), ChoicesResponse.from(i.choices()),
                    i.status(), i.orderedAt(), i.byStaff(), i.placedByName());
        }
    }

    @Schema(name = "StaffOrderRodizio")
    record Rodizio(
            @Schema(requiredMode = REQUIRED) String name,
            @Schema(requiredMode = REQUIRED, description = "Os produtos que podem entrar nas rodadas")
            List<UUID> productIds
    ) {
    }

    @Schema(name = "StaffOrderSheet")
    record Sheet(
            @Schema(requiredMode = REQUIRED) UUID tableId,
            @Schema(requiredMode = REQUIRED) String tableLabel,
            @Schema(requiredMode = REQUIRED, description = "O restaurante liberou o lançamento pela equipe")
            boolean orderingEnabled,
            @Schema(requiredMode = REQUIRED, description = "O que conta para a trava de pedido duplicado")
            List<Item> alreadyOrdered,
            @Schema(description = "O rodízio ativo na comanda; ausente se não houver") Rodizio rodizio,
            @Schema(description = "No balcão (ADR 0015): como a equipe chama o cliente") String counterName,
            @Schema(description = "No balcão: o número do cartão, se houver") String counterCard
    ) {
        static Sheet from(StaffOrderSheet s) {
            return new Sheet(s.tableId().value(), s.tableLabel(), s.orderingEnabled(),
                    s.alreadyOrdered().stream().map(Item::from).toList(),
                    s.rodizio() == null ? null : new Rodizio(s.rodizio().name(),
                            s.rodizio().productIds().stream().map(ProductId::value).toList()),
                    s.counter() == null ? null : s.counter().name(), s.counter() == null ? null : s.counter().card());
        }
    }

    @Schema(name = "DuplicateOrderProblem", description = "O envio parou: a mesa já pediu estes itens")
    record DuplicateProblem(
            @Schema(requiredMode = REQUIRED) String detail,
            @Schema(requiredMode = REQUIRED) List<Item> duplicates
    ) {
    }
}
