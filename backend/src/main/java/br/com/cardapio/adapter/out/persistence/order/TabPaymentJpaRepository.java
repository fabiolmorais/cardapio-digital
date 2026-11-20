package br.com.cardapio.adapter.out.persistence.order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface TabPaymentJpaRepository extends JpaRepository<TabPaymentJpaEntity, UUID> {

    List<TabPaymentJpaEntity> findByEstablishmentIdAndTabIdOrderByReceivedAtAsc(UUID establishmentId, UUID tabId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(nativeQuery = true, value = """
            UPDATE tab_payment SET tab_id = :toTab WHERE tab_id = :fromTab AND establishment_id = :establishmentId
            """)
    int moveToTab(@Param("establishmentId") UUID establishmentId, @Param("fromTab") UUID fromTab,
                  @Param("toTab") UUID toTab);
}
