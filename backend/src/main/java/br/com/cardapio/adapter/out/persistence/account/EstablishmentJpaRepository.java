package br.com.cardapio.adapter.out.persistence.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface EstablishmentJpaRepository extends JpaRepository<EstablishmentJpaEntity, UUID> {

    boolean existsBySlug(String slug);

    @Query("SELECT e.name FROM EstablishmentJpaEntity e WHERE e.id = :id")
    Optional<String> findNameById(@Param("id") UUID id);
}
