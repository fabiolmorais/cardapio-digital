package br.com.cardapio.adapter.out.persistence.rodizio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface RodizioProductJpaRepository extends JpaRepository<RodizioProductJpaEntity, RodizioProductJpaEntity.Key> {

    List<RodizioProductJpaEntity> findByRodizioIdIn(Collection<UUID> rodizioIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM RodizioProductJpaEntity p WHERE p.rodizioId = :rodizioId")
    void deleteByRodizio(@Param("rodizioId") UUID rodizioId);
}
