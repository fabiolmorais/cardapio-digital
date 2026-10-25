package br.com.cardapio.adapter.out.persistence.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "app_user")
class UserJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private boolean active;

    protected UserJpaEntity() {
    }

    UserJpaEntity(UUID id, String name, String email, String passwordHash, boolean active) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.active = active;
    }

    UUID getId() { return id; }
}
