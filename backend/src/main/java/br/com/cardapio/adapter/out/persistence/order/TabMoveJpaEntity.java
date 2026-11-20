package br.com.cardapio.adapter.out.persistence.order;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Uma mudança de mesa da comanda (ADR 0012). Só a via impressa muda depois (needs_print, printed_at). */
@Entity
@Table(name = "tab_move")
class TabMoveJpaEntity extends EstablishmentScopedEntity {

    @Id
    private UUID id;

    @Column(name = "tab_id", nullable = false)
    private UUID tabId;

    @Column(name = "from_table_id", nullable = false)
    private UUID fromTableId;

    @Column(name = "to_table_id", nullable = false)
    private UUID toTableId;

    @Column(name = "merged_tab_id")
    private UUID mergedTabId;

    @Column(name = "moved_by", nullable = false)
    private UUID movedBy;

    @Column(name = "moved_at", nullable = false)
    private Instant movedAt;

    @Column(name = "needs_print", nullable = false)
    private boolean needsPrint;

    @Column(name = "printed_at")
    private Instant printedAt;

    protected TabMoveJpaEntity() {
    }

    TabMoveJpaEntity(UUID id, UUID establishmentId, UUID tabId, UUID fromTableId, UUID toTableId, UUID mergedTabId,
                     UUID movedBy, Instant movedAt) {
        super(establishmentId);
        this.id = id;
        this.tabId = tabId;
        this.fromTableId = fromTableId;
        this.toTableId = toTableId;
        this.mergedTabId = mergedTabId;
        this.movedBy = movedBy;
        this.movedAt = movedAt;
    }
}
