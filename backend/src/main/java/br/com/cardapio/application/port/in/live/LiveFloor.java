package br.com.cardapio.application.port.in.live;

import br.com.cardapio.domain.call.model.CallStatus;
import br.com.cardapio.domain.call.model.CallType;
import br.com.cardapio.domain.call.model.PaymentMethod;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.floor.model.SectorId;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.shared.model.Money;

import java.time.Instant;
import java.util.List;

/**
 * O salão agora (desenhos 7.1 e 7.2): cada mesa com o que está acontecendo nela, e o resumo.
 *
 * @param revenueToday       o faturado de hoje (comandas fechadas); null para quem não é dono ou
 *                           gerente
 * @param averageWaitSeconds a espera média dos chamados de hoje; null se ninguém chamou
 */
public record LiveFloor(List<Table> tables, int occupied, Money revenueToday, Long averageWaitSeconds) {

    public LiveFloor {
        tables = List.copyOf(tables);
    }

    /**
     * @param state         o que pinta a mesa no mapa
     * @param call          o chamado aberto que mais importa (o mais antigo); null se nenhum
     * @param waitingOrders pedidos parados na confirmação do garçom
     */
    public record Table(DiningTableId id, String label, SectorId sectorId, String sectorName, State state,
                        int phones, TabId tabId, Instant tabOpenedAt, Money tabTotal, int itemsOnTheWay,
                        int rodizioPeople, Call call, int waitingOrders) {
    }

    /** O chamado aberto da mesa, como o mapa mostra. */
    public record Call(CallType type, CallStatus status, PaymentMethod paymentMethod, Instant createdAt,
                       String acceptedByName) {
    }

    /**
     * FREE: ninguém nem comanda. OCCUPIED: com gente, comanda ou um chamado já em atendimento.
     * CALLING: um chamado esperando alguém aceitar (garçom, liberação, rodízio). BILL: pediu a conta. A conta pedida vem antes do chamado comum: é ela que
     * o garçom precisa ver de longe.
     */
    public enum State { FREE, OCCUPIED, CALLING, BILL }
}
