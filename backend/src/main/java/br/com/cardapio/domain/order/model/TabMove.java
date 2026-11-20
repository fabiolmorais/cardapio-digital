package br.com.cardapio.domain.order.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.order.exception.TabMoveException;
import br.com.cardapio.domain.order.exception.TabStateException;

import java.time.Instant;
import java.util.Objects;

/**
 * A comanda mudou de mesa (ADR 0012). Se a mesa de destino já tinha comanda aberta, as duas se
 * juntaram: {@code tabId} é a que continuou (a do destino) e {@code mergedTabId} a que deixou de
 * existir (a de origem). Fica registrada para sempre, no detalhe da comanda.
 *
 * @param mergedTabId a comanda de origem, juntada à de destino; null se a mesa de destino estava livre
 */
public record TabMove(
        TabMoveId id,
        EstablishmentId establishmentId,
        TabId tabId,
        DiningTableId fromTableId,
        DiningTableId toTableId,
        TabId mergedTabId,
        UserId movedBy,
        Instant movedAt
) {

    public TabMove {
        Objects.requireNonNull(id);
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(tabId);
        Objects.requireNonNull(fromTableId);
        Objects.requireNonNull(toTableId);
        Objects.requireNonNull(movedBy);
        Objects.requireNonNull(movedAt);
        if (fromTableId.equals(toTableId)) {
            throw new TabMoveException("A comanda já está nesta mesa. Escolha outra.");
        }
    }

    /** Juntou com a comanda da mesa de destino? */
    public boolean merged() {
        return mergedTabId != null;
    }

    /**
     * A mesa de destino está livre: a mesma comanda, na mesa nova.
     *
     * @return a mudança; a comanda na mesa nova é {@link #applyTo(Tab)}
     */
    public static TabMove toFreeTable(Tab tab, DiningTableId to, UserId by, Instant now) {
        requireOpen(tab);
        return new TabMove(TabMoveId.newId(), tab.establishmentId(), tab.id(), tab.tableId(), to, null, by, now);
    }

    /** A mesa de destino tem comanda aberta: a de origem se junta a ela. */
    public static TabMove merge(Tab source, Tab destination, UserId by, Instant now) {
        requireOpen(source);
        requireOpen(destination);
        if (!source.establishmentId().equals(destination.establishmentId())) {
            throw new IllegalArgumentException("comandas de restaurantes diferentes");
        }
        if (source.id().equals(destination.id())) {
            throw new TabMoveException("A comanda já está nesta mesa. Escolha outra.");
        }
        return new TabMove(TabMoveId.newId(), source.establishmentId(), destination.id(), source.tableId(),
                destination.tableId(), source.id(), by, now);
    }

    /**
     * A comanda que continua, depois da mudança: na mudança simples, a de origem na mesa nova; na
     * junção, a de destino, aberta desde a mais antiga das duas.
     *
     * @param other a outra comanda da junção (a de origem); null na mudança simples
     */
    public Tab applyTo(Tab survivor, Tab other) {
        if (!survivor.id().equals(tabId)) {
            throw new IllegalArgumentException("esta não é a comanda que continua");
        }
        Instant openedAt = merged() && other != null && other.openedAt().isBefore(survivor.openedAt())
                ? other.openedAt() : survivor.openedAt();
        return new Tab(survivor.id(), survivor.establishmentId(), toTableId, survivor.status(), openedAt,
                survivor.closedAt(), survivor.closedBy(), survivor.total(), survivor.paymentMethod(), survivor.service(),
                survivor.serviceCharged());
    }

    private static void requireOpen(Tab tab) {
        if (!tab.status().isOpen()) {
            throw new TabStateException("Esta comanda já foi fechada. Atualize a tela.");
        }
    }
}
