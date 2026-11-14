package br.com.cardapio.adapter.out.persistence.order;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** Uma escolha de um item do pedido, copiada na hora (ADR 0010): o pedido não muda com o cardápio. */
@Entity
@Table(name = "order_item_option")
class OrderItemOptionJpaEntity extends EstablishmentScopedEntity {

    @Id
    private UUID id;

    @Column(name = "order_item_id", nullable = false)
    private UUID orderItemId;

    @Column(name = "option_group_id", nullable = false)
    private UUID optionGroupId;

    @Column(name = "option_item_id", nullable = false)
    private UUID optionItemId;

    @Column(name = "group_title", nullable = false)
    private String groupTitle;

    @Column(name = "option_name", nullable = false)
    private String optionName;

    @Column(name = "splits_item", nullable = false)
    private boolean splitsItem;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "price_cents", nullable = false)
    private long priceCents;

    @Column(name = "charged_cents", nullable = false)
    private long chargedCents;

    @Column(nullable = false)
    private int position;

    protected OrderItemOptionJpaEntity() {
    }

    OrderItemOptionJpaEntity(UUID id, UUID establishmentId, UUID orderItemId, UUID optionGroupId, UUID optionItemId,
                             String groupTitle, String optionName, boolean splitsItem, int quantity, long priceCents,
                             long chargedCents, int position) {
        super(establishmentId);
        this.id = id;
        this.orderItemId = orderItemId;
        this.optionGroupId = optionGroupId;
        this.optionItemId = optionItemId;
        this.groupTitle = groupTitle;
        this.optionName = optionName;
        this.splitsItem = splitsItem;
        this.quantity = quantity;
        this.priceCents = priceCents;
        this.chargedCents = chargedCents;
        this.position = position;
    }

    UUID getId() { return id; }
    UUID getOrderItemId() { return orderItemId; }
    UUID getOptionGroupId() { return optionGroupId; }
    UUID getOptionItemId() { return optionItemId; }
    String getGroupTitle() { return groupTitle; }
    String getOptionName() { return optionName; }
    boolean isSplitsItem() { return splitsItem; }
    int getQuantity() { return quantity; }
    long getPriceCents() { return priceCents; }
    long getChargedCents() { return chargedCents; }
    int getPosition() { return position; }
}
