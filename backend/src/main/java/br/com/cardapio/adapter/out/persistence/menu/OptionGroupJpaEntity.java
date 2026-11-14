package br.com.cardapio.adapter.out.persistence.menu;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** Um grupo de opções da biblioteca do restaurante (ADR 0010). */
@Entity
@Table(name = "option_group")
class OptionGroupJpaEntity extends EstablishmentScopedEntity {

    @Id
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "min_choices", nullable = false)
    private int minChoices;

    @Column(name = "max_choices", nullable = false)
    private int maxChoices;

    @Column(name = "max_per_option", nullable = false)
    private int maxPerOption;

    @Column(name = "splits_item", nullable = false)
    private boolean splitsItem;

    @Column(name = "pricing", nullable = false)
    private String pricing;

    @Column(name = "free_quantity", nullable = false)
    private int freeQuantity;

    @Column(name = "active", nullable = false)
    private boolean active;

    protected OptionGroupJpaEntity() {
    }

    OptionGroupJpaEntity(UUID id, UUID establishmentId, String name, String title, int minChoices, int maxChoices, int maxPerOption, boolean splitsItem, String pricing, int freeQuantity, boolean active) {
        super(establishmentId);
        this.id = id;
        this.name = name;
        this.title = title;
        this.minChoices = minChoices;
        this.maxChoices = maxChoices;
        this.maxPerOption = maxPerOption;
        this.splitsItem = splitsItem;
        this.pricing = pricing;
        this.freeQuantity = freeQuantity;
        this.active = active;
    }

    UUID getId() { return id; }
    String getName() { return name; }
    String getTitle() { return title; }
    int getMinChoices() { return minChoices; }
    int getMaxChoices() { return maxChoices; }
    int getMaxPerOption() { return maxPerOption; }
    boolean isSplitsItem() { return splitsItem; }
    String getPricing() { return pricing; }
    int getFreeQuantity() { return freeQuantity; }
    boolean isActive() { return active; }
}
