package br.com.cardapio.domain.order.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.call.model.PaymentMethod;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.order.exception.TabStateException;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.shared.model.Money;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * A comanda: junta os pedidos de todos os celulares da mesa, do primeiro pedido até ser fechada.
 * Ela abre sozinha no primeiro pedido (especificação); quem fecha é o garçom, depois do pagamento.
 *
 * @param closedAt      null enquanto não foi fechada
 * @param closedBy      quem fechou; null enquanto não foi fechada
 * @param total         o que foi cobrado, gravado ao fechar; null enquanto aberta
 * @param paymentMethod como a mesa pagou (a forma do último pagamento); null enquanto aberta, ou se
 *                      fechou sem nada a cobrar
 */
public record Tab(
        TabId id,
        EstablishmentId establishmentId,
        DiningTableId tableId,
        TabStatus status,
        Instant openedAt,
        Instant closedAt,
        UserId closedBy,
        Money total,
        PaymentMethod paymentMethod
) {

    public Tab {
        Objects.requireNonNull(id);
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(tableId);
        Objects.requireNonNull(status);
        Objects.requireNonNull(openedAt);
    }

    public static Tab open(EstablishmentId establishmentId, DiningTableId tableId, Instant now) {
        return new Tab(TabId.newId(), establishmentId, tableId, TabStatus.OPEN, now, null, null, null, null);
    }

    /**
     * O que vai para a conta: os itens que não foram cancelados (o recusado pelo garçom também é
     * cancelado). É a mesma soma que o cliente vê como "total parcial".
     */
    public static Money totalOf(List<OrderItem> items) {
        return items.stream()
                .filter(i -> i.status() != OrderItemStatus.CANCELED)
                .map(OrderItem::subtotal)
                .reduce(Money.ZERO, Money::plus);
    }

    /** Fecha a comanda de uma mesa sem rodízio e sem pagamento adiantado. */
    public Tab close(UserId by, PaymentMethod method, List<OrderItem> items, Instant now) {
        return close(by, method, items, Money.ZERO, List.of(), now).tab();
    }

    /** Fecha a comanda sem pagamento adiantado. */
    public Tab close(UserId by, PaymentMethod method, List<OrderItem> items, Money rodizio, Instant now) {
        return close(by, method, items, rodizio, List.of(), now).tab();
    }

    /**
     * O fechamento: a comanda fechada e, se ainda faltava pagar, o último pagamento (null se a mesa
     * já tinha pago tudo antes).
     */
    public record Closing(Tab tab, TabPayment finalPayment) {
    }

    /**
     * O garçom fecha, depois do pagamento. Não fecha com item ainda a caminho (na fila, em preparo
     * ou pronto): cobraria o que a mesa não recebeu. O garçom entrega ou cancela antes.
     *
     * Se a mesa já pagou adiantado, o fechamento cobra só o que falta; se não falta nada, não pede a
     * forma de pagamento (a mesa já informou ao pagar) e a comanda guarda a forma do último
     * pagamento.
     *
     * @param method   como a mesa pagou o que faltava; pode ser null se não falta nada
     * @param items    todos os itens da comanda
     * @param rodizio  o que o rodízio soma (pessoas × preço); zero se a mesa não está no rodízio
     * @param payments os pagamentos já recebidos, do mais antigo ao mais novo
     */
    public Closing close(UserId by, PaymentMethod method, List<OrderItem> items, Money rodizio,
                         List<TabPayment> payments, Instant now) {
        Objects.requireNonNull(by);
        if (!status.isOpen()) {
            throw new TabStateException("Esta comanda já foi fechada");
        }
        List<String> onTheWay = items.stream()
                .filter(i -> i.status().isOnTheWay())
                .map(i -> i.quantity() + "× " + i.productName())
                .toList();
        if (!onTheWay.isEmpty()) {
            throw new TabStateException("Ainda há itens a caminho: " + String.join(", ", onTheWay)
                    + ". Entregue ou cancele antes de fechar.");
        }
        TabBalance balance = TabBalance.of(items, rodizio, payments);
        TabPayment last = null;
        PaymentMethod main = method;
        if (!balance.settled()) {
            if (method == null) {
                throw new InvalidValueException("Escolha como a mesa pagou: Pix, cartão ou dinheiro");
            }
            last = new TabPayment(TabPaymentId.newId(), establishmentId, id, balance.due(), method, now, by, null);
        } else if (!payments.isEmpty()) {
            main = payments.getLast().method();
        }
        return new Closing(new Tab(id, establishmentId, tableId, TabStatus.CLOSED, openedAt, now, by,
                balance.total(), main), last);
    }
}
