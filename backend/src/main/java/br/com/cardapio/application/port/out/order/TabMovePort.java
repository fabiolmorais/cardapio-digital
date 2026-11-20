package br.com.cardapio.application.port.out.order;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.order.model.TabMove;
import br.com.cardapio.domain.order.model.TabMoveId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** O registro das mudanças de mesa (ADR 0012). */
public interface TabMovePort {

    void save(TabMove move);

    /**
     * Guarda, na mudança, os itens a caminho desta comanda que já tinham saído na impressora com a
     * mesa antiga, e marca a via para imprimir. Devolve false se não havia nenhum (sem via).
     */
    boolean recordPrintedOnTheWay(EstablishmentId establishmentId, TabMoveId moveId, TabId fromTab);

    /** As mudanças da comanda, da mais antiga à mais nova, com as mesas e quem fez. */
    List<TabMoveView> findByTab(EstablishmentId establishmentId, TabId tabId);

    /** A comanda a que esta se juntou, se foi juntada. */
    Optional<TabId> findMergedInto(EstablishmentId establishmentId, TabId tabId);

    /**
     * Alguma comanda foi da mesa {@code from} para a {@code to} desde aquele instante? Serve para
     * levar à mesa nova o celular que veio na mudança e abriu de novo o QR da mesa antiga.
     */
    boolean movedBetween(EstablishmentId establishmentId, DiningTableId from, DiningTableId to, Instant since);
}
