package br.com.cardapio.adapter.out.persistence.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "organization")
class OrganizationJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String document;

    @Column(nullable = false)
    private String whatsapp;

    @Column(nullable = false)
    private String email;

    protected OrganizationJpaEntity() {
    }

    OrganizationJpaEntity(UUID id, String name, String document, String whatsapp, String email) {
        this.id = id;
        this.name = name;
        this.document = document;
        this.whatsapp = whatsapp;
        this.email = email;
    }

    UUID getId() { return id; }
}
