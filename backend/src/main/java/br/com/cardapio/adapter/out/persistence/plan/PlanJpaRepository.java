package br.com.cardapio.adapter.out.persistence.plan;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface PlanJpaRepository extends JpaRepository<PlanJpaEntity, UUID> {

    Page<PlanJpaEntity> findByActiveTrueOrderByDisplayOrderAsc(Pageable pageable);
}
