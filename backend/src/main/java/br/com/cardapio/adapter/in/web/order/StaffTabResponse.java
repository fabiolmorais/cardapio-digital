package br.com.cardapio.adapter.in.web.order;

import br.com.cardapio.adapter.in.web.rodizio.TableRodizioResponse;
import br.com.cardapio.application.port.in.order.StaffTab;
import br.com.cardapio.domain.call.model.PaymentMethod;
import br.com.cardapio.domain.order.model.OrderStatus;
import br.com.cardapio.domain.order.model.TabStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "StaffTab")
record StaffTabResponse(
        @Schema(requiredMode = REQUIRED) UUID id,
        @Schema(requiredMode = REQUIRED, description = "A mesa (para ativar o rodízio ou ajustar as pessoas)")
        UUID tableId,
        @Schema(requiredMode = REQUIRED) String tableLabel,
        @Schema(requiredMode = REQUIRED) String sectorName,
        @Schema(requiredMode = REQUIRED) TabStatus status,
        @Schema(requiredMode = REQUIRED) Instant openedAt,
        Instant closedAt,
        @Schema(requiredMode = REQUIRED, description = "Aberta: a soma de agora. Fechada: o que foi cobrado")
        long totalCents,
        @Schema(requiredMode = REQUIRED, description = "O que a mesa já pagou (adiantado, ou tudo, se fechada)")
        long paidCents,
        @Schema(requiredMode = REQUIRED, description = "O que falta receber; zero se fechada") long dueCents,
        @Schema(requiredMode = REQUIRED, description = "O que a mesa pagou além do consumo (devolver ao fechar)")
        long overpaidCents,
        @Schema(requiredMode = REQUIRED, description = "Os pagamentos recebidos, do mais antigo ao mais novo")
        List<Payment> payments,
        @Schema(description = "Fechada: como a mesa pagou (a forma do último pagamento)") PaymentMethod paymentMethod,
        @Schema(description = "Aberta, com a conta pedida: a forma que a mesa escolheu")
        PaymentMethod billRequestedWith,
        @Schema(description = "O rodízio da comanda (já somado no total); ausente se não houve")
        TableRodizioResponse rodizio,
        @Schema(requiredMode = REQUIRED) List<Order> orders
) {

    @Schema(name = "TabPayment")
    record Payment(
            @Schema(requiredMode = REQUIRED) long amountCents,
            @Schema(requiredMode = REQUIRED) PaymentMethod paymentMethod,
            @Schema(requiredMode = REQUIRED) Instant receivedAt
    ) {
    }

    @Schema(name = "StaffTabOrder")
    record Order(
            @Schema(requiredMode = REQUIRED) UUID id,
            @Schema(requiredMode = REQUIRED) int number,
            @Schema(requiredMode = REQUIRED) OrderStatus status,
            @Schema(requiredMode = REQUIRED) Instant createdAt,
            @Schema(requiredMode = REQUIRED) List<TableOrderResponse.Item> items
    ) {
    }

    static StaffTabResponse from(StaffTab t) {
        return new StaffTabResponse(t.id().value(), t.tableId().value(), t.tableLabel(), t.sectorName(), t.status(),
                t.openedAt(), t.closedAt(), t.total().cents(), t.paid().cents(), t.due().cents(),
                t.overpaid().cents(),
                t.payments().stream().map(p -> new Payment(p.amount().cents(), p.method(), p.receivedAt())).toList(),
                t.paymentMethod(), t.billRequestedWith(),
                t.rodizio() == null ? null : TableRodizioResponse.from(t.rodizio()),
                t.orders().stream().map(o -> new Order(o.id().value(), o.number(), o.status(), o.createdAt(),
                        o.items().stream().map(TableOrderResponse.Item::from).toList())).toList());
    }
}
