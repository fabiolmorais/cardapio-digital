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

    /** Vazio para quem entra só por PIN (garçom, cozinha). */
    @Column
    private String email;

    @Column(name = "password_hash")
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
    String getName() { return name; }
    String getEmail() { return email; }
    String getPasswordHash() { return passwordHash; }
    boolean isActive() { return active; }
}
