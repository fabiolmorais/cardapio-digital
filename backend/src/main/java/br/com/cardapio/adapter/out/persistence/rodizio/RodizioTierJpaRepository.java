package br.com.cardapio.adapter.out.persistence.rodizio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface RodizioTierJpaRepository extends JpaRepository<RodizioTierJpaEntity, UUID> {

    List<RodizioTierJpaEntity> findByRodizioIdInOrderByPositionAsc(Collection<UUID> rodizioIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM RodizioTierJpaEntity t WHERE t.rodizioId = :rodizioId")
    void deleteByRodizio(@Param("rodizioId") UUID rodizioId);
}
