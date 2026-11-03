package br.com.cardapio.adapter.out.persistence.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface TabJpaRepository extends JpaRepository<TabJpaEntity, UUID> {

    @Query("""
            SELECT t FROM TabJpaEntity t
             WHERE t.establishmentId = :establishmentId AND t.tableId = :tableId AND t.status IN ('OPEN', 'CLOSING')
            """)
    Optional<TabJpaEntity> findOpen(@Param("establishmentId") UUID establishmentId, @Param("tableId") UUID tableId);

    /** Abre a comanda, a menos que o índice "uma aberta por mesa" já tenha uma (aí não grava: 0 linhas). */
    @Modifying
    @Query(nativeQuery = true, value = """
            INSERT INTO tab (id, establishment_id, table_id, status, opened_at)
            VALUES (:id, :establishmentId, :tableId, 'OPEN', :openedAt)
            ON CONFLICT (table_id) WHERE status IN ('OPEN', 'CLOSING') DO NOTHING
            """)
    int insertIfNoneOpen(@Param("id") UUID id, @Param("establishmentId") UUID establishmentId,
                         @Param("tableId") UUID tableId, @Param("openedAt") Instant openedAt);
}
