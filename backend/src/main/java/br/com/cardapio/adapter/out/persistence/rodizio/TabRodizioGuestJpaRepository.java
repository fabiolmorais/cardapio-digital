package br.com.cardapio.adapter.out.persistence.rodizio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface TabRodizioGuestJpaRepository extends JpaRepository<TabRodizioGuestJpaEntity, TabRodizioGuestJpaEntity.Key> {

    List<TabRodizioGuestJpaEntity> findByTabRodizioIdOrderByPositionAsc(UUID tabRodizioId);

    /** Muda só a quantidade: a faixa e o preço copiado ficam como estavam. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE TabRodizioGuestJpaEntity g SET g.quantity = :quantity
             WHERE g.tabRodizioId = :tabRodizioId AND g.position = :position
               AND g.establishmentId = :establishmentId
            """)
    int updateQuantity(@Param("tabRodizioId") UUID tabRodizioId, @Param("position") int position,
                       @Param("establishmentId") UUID establishmentId, @Param("quantity") int quantity);
}
