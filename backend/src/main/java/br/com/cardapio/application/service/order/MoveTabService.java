package br.com.cardapio.application.service.order;

import br.com.cardapio.application.port.in.order.MoveTabCommand;
import br.com.cardapio.application.port.in.order.MoveTabUseCase;
import br.com.cardapio.application.port.in.order.MovedTab;
import br.com.cardapio.application.port.out.call.CallPort;
import br.com.cardapio.application.port.out.call.CallView;
import br.com.cardapio.application.port.out.floor.DiningTablePort;
import br.com.cardapio.application.port.out.order.OrderPort;
import br.com.cardapio.application.port.out.order.TabMovePort;
import br.com.cardapio.application.port.out.order.TabPaymentPort;
import br.com.cardapio.application.port.out.order.TabPort;
import br.com.cardapio.application.port.out.order.TabView;
import br.com.cardapio.application.port.out.realtime.EventPublisherPort;
import br.com.cardapio.application.port.out.rodizio.RodizioPhonePort;
import br.com.cardapio.application.port.out.rodizio.TabRodizioPort;
import br.com.cardapio.application.port.out.tablesession.TableSessionPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.call.event.CallChanged;
import br.com.cardapio.domain.call.model.Call;
import br.com.cardapio.domain.call.model.CallType;
import br.com.cardapio.domain.floor.exception.TableNotFoundException;
import br.com.cardapio.domain.floor.model.DiningTable;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.order.event.TabMoved;
import br.com.cardapio.domain.order.exception.TabMoveException;
import br.com.cardapio.domain.order.exception.TabNotFoundException;
import br.com.cardapio.domain.order.exception.TabStateException;
import br.com.cardapio.domain.order.model.Tab;
import br.com.cardapio.domain.order.model.TabMove;
import br.com.cardapio.domain.rodizio.model.TabRodizio;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Mudar a comanda de mesa ou juntar com a da mesa de destino (RF44, ADR 0012). Tudo numa transação:
 * se uma parte falhar, nada muda.
 */
public class MoveTabService implements MoveTabUseCase {

    private final TabPort tabs;
    private final DiningTablePort tables;
    private final OrderPort orders;
    private final TabPaymentPort payments;
    private final TabRodizioPort tabRodizios;
    private final RodizioPhonePort rodizioPhones;
    private final CallPort calls;
    private final TableSessionPort sessions;
    private final TabMovePort moves;
    private final EventPublisherPort events;
    private final Clock clock;

    public MoveTabService(TabPort tabs, DiningTablePort tables, OrderPort orders, TabPaymentPort payments,
                          TabRodizioPort tabRodizios, RodizioPhonePort rodizioPhones, CallPort calls,
                          TableSessionPort sessions, TabMovePort moves, EventPublisherPort events, Clock clock) {
        this.tabs = tabs;
        this.tables = tables;
        this.orders = orders;
        this.payments = payments;
        this.tabRodizios = tabRodizios;
        this.rodizioPhones = rodizioPhones;
        this.calls = calls;
        this.sessions = sessions;
        this.moves = moves;
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional
    public MovedTab move(MoveTabCommand command) {
        Instant now = clock.instant();
        EstablishmentId establishmentId = command.establishmentId();
        TabView view = tabs.findView(establishmentId, command.tabId()).orElseThrow(TabNotFoundException::new);
        Tab source = view.tab();
        DiningTable to = tables.find(establishmentId, command.toTableId()).orElseThrow(TableNotFoundException::new);
        if (!source.status().isOpen()) {
            throw new TabStateException("Esta comanda já foi fechada. Atualize a tela.");
        }
        if (!to.active()) {
            throw new TabMoveException("A Mesa " + to.label() + " está desativada. Escolha outra.");
        }
        if (source.tableId().equals(to.id())) {
            throw new TabMoveException("A comanda já está nesta mesa. Escolha outra.");
        }

        // Trava as comandas envolvidas, sempre na mesma ordem (dois garçons juntando 4 com 12 e 12
        // com 4 ao mesmo tempo não se travam um ao outro). Se uma fechou no meio, o garçom escolhe de novo.
        Optional<Tab> destination = tabs.findOpen(establishmentId, to.id());
        Stream.concat(Stream.of(source), destination.stream())
                .sorted(Comparator.comparing(tab -> tab.id().value()))
                .forEach(tab -> {
                    if (!tabs.lockOpen(establishmentId, tab.id())) {
                        throw new TabStateException("Uma das comandas acabou de mudar. Atualize a tela e tente de novo.");
                    }
                });

        TabMove move = destination
                .map(d -> TabMove.merge(source, d, command.by(), now))
                .orElseGet(() -> TabMove.toFreeTable(source, to.id(), command.by(), now));
        // O rodízio é conferido antes de gravar qualquer coisa: rodízios diferentes não se juntam.
        Runnable rodizio = destination.map(d -> rodizioMerge(establishmentId, source, d)).orElse(() -> { });

        moves.save(move);
        // A via "MUDOU DE MESA" leva os itens da comanda de origem que já saíram no papel com a mesa antiga.
        moves.recordPrintedOnTheWay(establishmentId, move.id(), source.id());

        Tab survivor;
        if (move.merged()) {
            Tab kept = destination.orElseThrow();
            rodizio.run();
            orders.moveToTab(establishmentId, source.id(), kept.id(), kept.tableId());
            payments.moveToTab(establishmentId, source.id(), kept.id());
            survivor = move.applyTo(kept, source);
            if (!tabs.markMerged(establishmentId, source.id(), kept.id()) || !tabs.saveMoved(survivor)) {
                throw new TabStateException("Uma das comandas acabou de mudar. Atualize a tela e tente de novo.");
            }
        } else {
            survivor = move.applyTo(source, null);
            // A mesa livre pode ter acabado de ganhar uma comanda (um pedido chegou agora): o índice
            // "uma aberta por mesa" recusa, e o garçom escolhe de novo (vai juntar, desta vez).
            if (!tabs.saveMoved(survivor)) {
                throw new TabStateException("A Mesa " + to.label() + " acabou de receber uma comanda. Tente de novo.");
            }
            orders.moveToTab(establishmentId, source.id(), source.id(), to.id());
        }

        moveCalls(establishmentId, move.fromTableId(), move.toTableId(), now);
        sessions.moveTable(establishmentId, move.fromTableId(), move.toTableId(), now);

        events.publish(new TabMoved(establishmentId, move.fromTableId(), move.fromTableId(), move.toTableId(),
                survivor.id(), now));
        events.publish(new TabMoved(establishmentId, move.toTableId(), move.fromTableId(), move.toTableId(),
                survivor.id(), now));
        return new MovedTab(survivor.id(), survivor.tableId(), move.merged());
    }

    /**
     * O que acontece com o rodízio na junção, já conferido (pode recusar a junção aqui). Só uma das
     * comandas tem rodízio: a junta fica com ele. As duas no mesmo: as pessoas se somam.
     */
    private Runnable rodizioMerge(EstablishmentId establishmentId, Tab source, Tab destination) {
        TabRodizio from = tabRodizios.findByTab(establishmentId, source.id()).orElse(null);
        TabRodizio kept = tabRodizios.findByTab(establishmentId, destination.id()).orElse(null);
        if (from == null) {
            return () -> { };
        }
        if (kept == null) {
            return () -> tabRodizios.moveToTab(from.movedTo(destination.id()));
        }
        TabRodizio sum = kept.absorb(from);
        return () -> {
            tabRodizios.updateGuests(sum);
            tabRodizios.moveRounds(establishmentId, from.id(), kept.id());
            rodizioPhones.moveTo(establishmentId, from.id(), kept.id());
        };
    }

    /**
     * Os chamados abertos vão para a mesa nova. Se as duas mesas tinham o mesmo chamado aberto (as
     * duas pediram a conta), fica o mais antigo, e o outro termina atendido junto.
     */
    private void moveCalls(EstablishmentId establishmentId, DiningTableId from, DiningTableId to, Instant now) {
        List<Call> there = calls.findOpenByTable(establishmentId, to).stream().map(CallView::call).toList();
        for (CallView open : calls.findOpenByTable(establishmentId, from)) {
            Call mine = open.call();
            if (mine.type() == CallType.UNLOCK) {
                continue; // a liberação é de cada celular, não da mesa: vai junto, sem conflito
            }
            there.stream().filter(other -> other.type() == mine.type()).findFirst().ifPresent(other -> {
                Call newer = other.createdAt().isAfter(mine.createdAt()) ? other : mine;
                Call done = newer.closeAsDuplicate(now);
                if (calls.update(done, newer.status())) {
                    events.publish(CallChanged.of(done, now));
                }
            });
        }
        calls.moveOpen(establishmentId, from, to);
    }
}
