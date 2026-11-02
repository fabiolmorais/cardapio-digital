package br.com.cardapio.adapter.out.persistence.tablesession;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface TableSessionJpaRepository extends JpaRepository<TableSessionJpaEntity, UUID> {

    Optional<TableSessionJpaEntity> findByTokenHash(String tokenHash);
}
