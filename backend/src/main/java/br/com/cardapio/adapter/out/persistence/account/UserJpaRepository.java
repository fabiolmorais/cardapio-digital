package br.com.cardapio.adapter.out.persistence.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

    boolean existsByEmail(String email);

    Optional<UserJpaEntity> findByEmail(String email);

    @Modifying
    @Query("UPDATE UserJpaEntity u SET u.passwordHash = :passwordHash WHERE u.id = :id")
    int changePassword(@Param("id") UUID id, @Param("passwordHash") String passwordHash);
}

