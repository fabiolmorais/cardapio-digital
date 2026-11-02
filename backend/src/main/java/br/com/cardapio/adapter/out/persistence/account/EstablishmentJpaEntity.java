package br.com.cardapio.adapter.out.persistence.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "establishment")
class EstablishmentJpaEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String slug;

    @Column(name = "time_zone", nullable = false)
    private String timeZone;

    @Column(nullable = false)
    private boolean active;

    // Marca e aparência (Guia 11). Os valores iniciais são os mesmos padrões da migração V11:
    // o cadastro (Guia 1) cria o restaurante sem saber nada de marca.
    @Column(name = "brand_color", nullable = false)
    private String brandColor = "TOMATO";

    @Column(nullable = false)
    private String theme = "DARK";

    @Column(name = "logo_path")
    private String logoPath;

    private String instagram;

    @Column(name = "google_review_url")
    private String googleReviewUrl;

    protected EstablishmentJpaEntity() {
    }

    EstablishmentJpaEntity(UUID id, UUID organizationId, String name, String slug, String timeZone, boolean active) {
        this.id = id;
        this.organizationId = organizationId;
        this.name = name;
        this.slug = slug;
        this.timeZone = timeZone;
        this.active = active;
    }

    UUID getId() { return id; }
    String getName() { return name; }
    String getBrandColor() { return brandColor; }
    String getTheme() { return theme; }
    String getLogoPath() { return logoPath; }
    String getInstagram() { return instagram; }
    String getGoogleReviewUrl() { return googleReviewUrl; }

    /**
     * Copia a marca para a linha. Entidade JPA pode ter um método assim (as regras ficam no
     * domínio, que já validou tudo); o Hibernate percebe a mudança e grava no fim da transação.
     */
    void applyBrand(String name, String brandColor, String theme, String logoPath, String instagram,
                    String googleReviewUrl) {
        this.name = name;
        this.brandColor = brandColor;
        this.theme = theme;
        this.logoPath = logoPath;
        this.instagram = instagram;
        this.googleReviewUrl = googleReviewUrl;
    }
}
