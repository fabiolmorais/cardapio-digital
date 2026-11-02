package br.com.cardapio.adapter.out.persistence.presence;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "presence_rule")
class PresenceRuleJpaEntity extends EstablishmentScopedEntity {

    @Id
    private UUID id;

    /** Um array do Postgres (VARCHAR[]): as redes são poucas e sempre lidas juntas com a regra. */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "varchar(50)[]")
    private String[] networks;

    @Column(name = "wifi_name")
    private String wifiName;

    private Double latitude;

    private Double longitude;

    @Column(name = "radius_meters")
    private Integer radiusMeters;

    @Column(name = "session_idle_minutes", nullable = false)
    private int sessionIdleMinutes;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PresenceRuleJpaEntity() {
    }

    PresenceRuleJpaEntity(UUID id, UUID establishmentId, String[] networks, String wifiName, Double latitude,
                          Double longitude, Integer radiusMeters, int sessionIdleMinutes, Instant updatedAt) {
        super(establishmentId);
        this.id = id;
        this.networks = networks;
        this.wifiName = wifiName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radiusMeters = radiusMeters;
        this.sessionIdleMinutes = sessionIdleMinutes;
        this.updatedAt = updatedAt;
    }

    UUID getId() { return id; }
    String[] getNetworks() { return networks; }
    String getWifiName() { return wifiName; }
    Double getLatitude() { return latitude; }
    Double getLongitude() { return longitude; }
    Integer getRadiusMeters() { return radiusMeters; }
    int getSessionIdleMinutes() { return sessionIdleMinutes; }
    Instant getUpdatedAt() { return updatedAt; }
}
