package br.com.cardapio.adapter.out.persistence.rodizio;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "rodizio_price")
class RodizioPriceJpaEntity extends EstablishmentScopedEntity {

    @Id
    private UUID id;

    @Column(name = "tier_id", nullable = false)
    private UUID tierId;

    /** Os dias como bits: segunda = 1, terça = 2, quarta = 4 … domingo = 64. */
    @Column(nullable = false)
    private short days;

    /**
     * O horário do relógio do restaurante, gravado como está. Sem o LOCAL_TIME, o Hibernate
     * converteria do fuso da JVM para UTC (o hibernate.jdbc.time_zone do application.yml), e
     * 18:00 viraria 21:00 numa máquina em São Paulo.
     */
    @JdbcTypeCode(SqlTypes.LOCAL_TIME)
    @Column(name = "starts_at", nullable = false)
    private LocalTime startsAt;

    @JdbcTypeCode(SqlTypes.LOCAL_TIME)
    @Column(name = "ends_at", nullable = false)
    private LocalTime endsAt;

    @Column(name = "price_cents", nullable = false)
    private long priceCents;

    @Column(nullable = false)
    private int position;

    protected RodizioPriceJpaEntity() {
    }

    RodizioPriceJpaEntity(UUID id, UUID establishmentId, UUID tierId, short days, LocalTime startsAt,
                          LocalTime endsAt, long priceCents, int position) {
        super(establishmentId);
        this.id = id;
        this.tierId = tierId;
        this.days = days;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.priceCents = priceCents;
        this.position = position;
    }

    UUID getTierId() { return tierId; }
    short getDays() { return days; }
    LocalTime getStartsAt() { return startsAt; }
    LocalTime getEndsAt() { return endsAt; }
    long getPriceCents() { return priceCents; }
}
