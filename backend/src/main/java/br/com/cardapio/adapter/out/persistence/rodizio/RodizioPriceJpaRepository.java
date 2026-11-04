package br.com.cardapio.adapter.out.persistence.rodizio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface RodizioPriceJpaRepository extends JpaRepository<RodizioPriceJpaEntity, UUID> {

    List<RodizioPriceJpaEntity> findByTierIdInOrderByPositionAsc(Collection<UUID> tierIds);

    /** Os preços especiais de todas as faixas do rodízio (que vão ser trocadas). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            DELETE FROM RodizioPriceJpaEntity p
             WHERE p.tierId IN (SELECT t.id FROM RodizioTierJpaEntity t WHERE t.rodizioId = :rodizioId)
            """)
    void deleteByRodizio(@Param("rodizioId") UUID rodizioId);
}
