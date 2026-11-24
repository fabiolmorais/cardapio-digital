package br.com.cardapio.application.port.in.order;

import br.com.cardapio.application.port.in.rodizio.TableRodizio;
import br.com.cardapio.application.port.out.order.OrderWithItems;
import br.com.cardapio.application.port.out.order.TabView;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.call.model.PaymentMethod;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.order.model.OrderId;
import br.com.cardapio.domain.order.model.OrderItem;
import br.com.cardapio.domain.order.model.OrderOrigin;
import br.com.cardapio.domain.order.model.OrderStatus;
import br.com.cardapio.domain.order.model.Tab;
import br.com.cardapio.domain.order.model.TabBalance;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.order.model.TabPayment;
import br.com.cardapio.domain.order.model.TabStatus;
import br.com.cardapio.domain.rodizio.model.TabRodizio;
import br.com.cardapio.domain.shared.model.Money;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import br.com.cardapio.application.port.out.order.TabMoveView;
import br.com.cardapio.domain.order.model.ServiceCharge;
import br.com.cardapio.domain.order.model.ServiceChargeSettings;

/**
 * A comanda como a EQUIPE vê: todos os pedidos, de todos os celulares, com o passo de cada item,
 * o total e, se a mesa pediu a conta, a forma de pagamento que escolheu.
 *
 * @param total             aberta: a soma de agora; fechada: o que foi cobrado
 * @param paid              o que a mesa já pagou (adiantado, ou tudo, se fechada)
 * @param due               o que falta receber; zero se fechada
 * @param overpaid          o que a mesa pagou além do consumo (um item cancelado depois do pagamento)
 * @param payments          os pagamentos recebidos, do mais antigo ao mais novo
 * @param paymentMethod     fechada: como pagou (a forma do último pagamento); null enquanto aberta
 * @param billRequestedWith aberta e com a conta pedida: a forma escolhida pela mesa; senão null
 * @param rodizio           o rodízio da comanda (já somado no total); null se não houve
 * @param moves             as mudanças de mesa, da mais antiga à mais nova (ADR 0012)
 * @param mergedInto        juntada (MERGED): a comanda que continuou; senão null
 * @param service           a taxa de serviço (RF39): aberta, a oferta; fechada, a que foi cobrada; null sem taxa
 */
public record StaffTab(TabId id, DiningTableId tableId, String tableLabel, String sectorName, TabStatus status,
                       Instant openedAt,
                       Instant closedAt, Money total, Money paid, Money due, Money overpaid, List<Payment> payments,
                       PaymentMethod paymentMethod, PaymentMethod billRequestedWith, TableRodizio rodizio,
                       List<Order> orders, List<Move> moves, TabId mergedInto, ServiceOffer service) {

    public StaffTab {
        payments = List.copyOf(payments);
        orders = List.copyOf(orders);
        moves = List.copyOf(moves);
    }

    /** Uma mudança de mesa: de qual para qual, se juntou, quem e quando (ADR 0012). */
    public record Move(String fromTableLabel, String toTableLabel, boolean merged, String movedByName,
                       Instant movedAt) {
    }

    /** A comanda com as mudanças de mesa e, se foi juntada, a que continuou. */
    public StaffTab withMoves(List<TabMoveView> newMoves, TabId newMergedInto) {
        return new StaffTab(id, tableId, tableLabel, sectorName, status, openedAt, closedAt, total, paid, due,
                overpaid, payments, paymentMethod, billRequestedWith, rodizio, orders,
                newMoves.stream().map(m -> new Move(m.fromTableLabel(), m.toTableLabel(), m.merged(),
                        m.movedByName(), m.movedAt())).toList(), newMergedInto, service);
    }

    /** Um pagamento recebido: quanto, como e quando. */
    public record Payment(Money amount, PaymentMethod method, Instant receivedAt) {
    }

    /**
     * @param byStaff            foi a equipe que lançou (senão, um celular da mesa)
     * @param placedByName       o nome de quem lançou; null se veio de um celular
     * @param duplicateConfirmed quem lançou viu o aviso de item repetido e confirmou
     */
    public record Order(OrderId id, int number, OrderStatus status, Instant createdAt, boolean byStaff,
                        String placedByName, boolean duplicateConfirmed, List<TableOrder.Item> items) {

        public Order {
            items = List.copyOf(items);
        }
    }

    /**
     * @param rodizio  o rodízio da comanda; null se não houve
     * @param payments os pagamentos da comanda, do mais antigo ao mais novo
     * @param staffNames o nome de quem lançou pedidos (ver {@link #staffOf})
     */
    public static StaffTab from(TabView view, List<OrderWithItems> found, PaymentMethod billRequestedWith,
                                TabRodizio rodizio, List<TabPayment> payments, Map<UserId, String> staffNames) {
        return from(view, found, billRequestedWith, rodizio, payments, staffNames, null, ServiceChargeSettings.DEFAULT, null);
    }

    /**
     * @param service     a taxa que vale para a comanda (a escolhida, ou a do restaurante, já aceita); null sem taxa
     * @param settings    a taxa do restaurante: para quem fica registrada e se aceita outro valor
     * @param waiterName  o garçom que fica com ela, na regra "garçom que atendeu"
     */
    public static StaffTab from(TabView view, List<OrderWithItems> found, PaymentMethod billRequestedWith,
                                TabRodizio rodizio, List<TabPayment> payments, Map<UserId, String> staffNames,
                                ServiceCharge service, ServiceChargeSettings settings, String waiterName) {
        Tab tab = view.tab();
        List<OrderItem> all = found.stream().flatMap(o -> o.items().stream()).toList();
        TabBalance balance = TabBalance.of(all, rodizio == null ? Money.ZERO : rodizio.total(), service, payments);
        boolean open = tab.status().isOpen();
        return new StaffTab(tab.id(), tab.tableId(), view.tableLabel(), view.sectorName(), tab.status(), tab.openedAt(),
                tab.closedAt(), tab.total() != null ? tab.total() : balance.total(), balance.paid(),
                open ? balance.due() : Money.ZERO, balance.overpaid(),
                payments.stream().map(p -> new Payment(p.amount(), p.method(), p.receivedAt())).toList(),
                tab.paymentMethod(), billRequestedWith,
                rodizio == null ? null : TableRodizio.from(rodizio),
                found.stream().map(o -> new Order(o.order().id(), o.order().number(), o.order().status(),
                        o.order().createdAt(), o.order().byStaff(),
                        o.order().origin() instanceof OrderOrigin.Staff staff ? staffNames.get(staff.userId()) : null,
                        o.order().duplicateConfirmed(),
                        o.items().stream().map(TableOrder.Item::from).toList())).toList(), List.of(), null,
                ServiceOffer.of(service, balance.total(), open ? balance.paid() : balance.total(), settings, waiterName));
    }

    /** Quem da equipe lançou estes pedidos. */
    public static Set<UserId> staffOf(List<OrderWithItems> found) {
        return found.stream()
                .map(o -> o.order().origin())
                .filter(OrderOrigin.Staff.class::isInstance)
                .map(origin -> ((OrderOrigin.Staff) origin).userId())
                .collect(Collectors.toSet());
    }

}
