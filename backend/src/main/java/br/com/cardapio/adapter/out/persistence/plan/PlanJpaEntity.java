package br.com.cardapio.adapter.out.persistence.plan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "plan")
class PlanJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "monthly_price_cents")
    private Long monthlyPriceCents;

    @Column(name = "annual_price_cents")
    private Long annualPriceCents;

    @Column(name = "max_tables")
    private Integer maxTables;

    @Column(name = "max_users")
    private Integer maxUsers;

    @Column(name = "max_sectors")
    private Integer maxSectors;

    @Column(name = "history_days", nullable = false)
    private int historyDays;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> entitlements;

    @Column(name = "support_level", nullable = false)
    private String supportLevel;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean active;

    protected PlanJpaEntity() {
    }

    UUID getId() { return id; }
    String getCode() { return code; }
    String getName() { return name; }
    Long getMonthlyPriceCents() { return monthlyPriceCents; }
    Long getAnnualPriceCents() { return annualPriceCents; }
    Integer getMaxTables() { return maxTables; }
    Integer getMaxUsers() { return maxUsers; }
    Integer getMaxSectors() { return maxSectors; }
    int getHistoryDays() { return historyDays; }
    List<String> getEntitlements() { return entitlements; }
    String getSupportLevel() { return supportLevel; }
    int getDisplayOrder() { return displayOrder; }
    boolean isActive() { return active; }
}
