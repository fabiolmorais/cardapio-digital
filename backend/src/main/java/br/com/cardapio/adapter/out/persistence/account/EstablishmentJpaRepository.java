package br.com.cardapio.adapter.out.persistence.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface EstablishmentJpaRepository extends JpaRepository<EstablishmentJpaEntity, UUID> {

    boolean existsBySlug(String slug);

    @Query("SELECT e.timeZone FROM EstablishmentJpaEntity e WHERE e.id = :id")
    Optional<String> findTimeZone(@Param("id") UUID id);
}
