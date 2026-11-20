package br.com.cardapio.domain.order.model;

import br.com.cardapio.domain.shared.model.Money;

import java.util.List;
import java.util.Objects;

/**
 * As contas da comanda: o consumo (itens não cancelados mais o rodízio), o que a mesa já pagou e o
 * que falta. Se um item foi cancelado depois do pagamento, a mesa pode ter pago a mais: o garçom
 * devolve a diferença (overpaid).
 *
 * @param total    o consumo
 * @param paid     a soma dos pagamentos recebidos
 * @param due      o que falta pagar (nunca negativo)
 * @param overpaid o que a mesa pagou além do consumo (zero quase sempre)
 */
public record TabBalance(Money total, Money service, Money paid, Money due, Money overpaid) {

    public TabBalance {
        Objects.requireNonNull(total);
        Objects.requireNonNull(service);
        Objects.requireNonNull(paid);
        Objects.requireNonNull(due);
        Objects.requireNonNull(overpaid);
    }

    /** @param rodizio o que o rodízio soma (pessoas × preço); zero se a mesa não está no rodízio */
    public static TabBalance of(List<OrderItem> items, Money rodizio, List<TabPayment> payments) {
        return of(items, rodizio, null, payments);
    }

    /**
     * Com a taxa de serviço (RF39): o que falta é o consumo mais a taxa, se a mesa aceitou, menos o
     * que já foi pago. O total continua sendo só o consumo (a taxa não é receita do restaurante).
     *
     * @param service a taxa da comanda; null se o restaurante não cobra
     */
    public static TabBalance of(List<OrderItem> items, Money rodizio, ServiceCharge service,
                                List<TabPayment> payments) {
        Money total = Tab.totalOf(items).plus(rodizio);
        Money charged = service == null ? Money.ZERO : service.chargedOn(total);
        Money bill = total.plus(charged);
        Money paid = payments.stream().map(TabPayment::amount).reduce(Money.ZERO, Money::plus);
        return new TabBalance(total, charged, paid, bill.minusOrZero(paid), paid.minusOrZero(bill));
    }

    /** Nada falta pagar: o garçom fecha sem cobrar mais nada. */
    public boolean settled() {
        return due.isZero();
    }
}
