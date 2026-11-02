package br.com.cardapio.adapter.out.persistence.call;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface CallJpaRepository extends JpaRepository<CallJpaEntity, UUID> {

    /**
     * Grava o chamado, a menos que o índice "um aberto por tipo por mesa" já tenha um: aí o banco
     * simplesmente não grava (DO NOTHING) e devolve 0 linhas. Diferente de deixar o erro do índice
     * estourar, a transação continua boa, e o caso de uso ainda pode ler o chamado que já existia.
     * O CAST diz ao banco o tipo do pagamento, que pode vir nulo.
     */
    @Modifying
    @Query(nativeQuery = true, value = """
            INSERT INTO call (id, establishment_id, table_id, session_id, type, payment_method, status, created_at)
            VALUES (:id, :establishmentId, :tableId, :sessionId, :type, CAST(:paymentMethod AS VARCHAR), 'PENDING',
                    :createdAt)
            ON CONFLICT (table_id, type) WHERE status IN ('PENDING', 'IN_PROGRESS') DO NOTHING
            """)
    int insertIfNoneOpen(@Param("id") UUID id, @Param("establishmentId") UUID establishmentId,
                         @Param("tableId") UUID tableId, @Param("sessionId") UUID sessionId, @Param("type") String type,
                         @Param("paymentMethod") String paymentMethod, @Param("createdAt") Instant createdAt);

    /**
     * Grava a mudança só se o status ainda for o esperado. Devolve quantas linhas mudaram: 1 ou 0.
     * flush e clear: o que estiver pendente vai antes, e nada antigo fica na memória depois.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE CallJpaEntity c
               SET c.paymentMethod = :paymentMethod, c.status = :status, c.acceptedAt = :acceptedAt,
                   c.acceptedBy = :acceptedBy, c.doneAt = :doneAt, c.canceledAt = :canceledAt
             WHERE c.id = :id AND c.establishmentId = :establishmentId AND c.status = :expected
            """)
    int updateIfStatus(@Param("id") UUID id, @Param("establishmentId") UUID establishmentId,
                       @Param("expected") String expected, @Param("paymentMethod") String paymentMethod,
                       @Param("status") String status, @Param("acceptedAt") Instant acceptedAt,
                       @Param("acceptedBy") UUID acceptedBy, @Param("doneAt") Instant doneAt,
                       @Param("canceledAt") Instant canceledAt);

    Optional<CallJpaEntity> findByIdAndEstablishmentId(UUID id, UUID establishmentId);

    /** O limite por minuto: o Spring Data monta o COUNT pelo nome do método. */
    long countByEstablishmentIdAndSessionIdAndCreatedAtAfter(UUID establishmentId, UUID sessionId, Instant instant);

    @Query("""
            SELECT c FROM CallJpaEntity c
             WHERE c.establishmentId = :establishmentId AND c.sessionId = :sessionId
               AND c.status IN ('PENDING', 'IN_PROGRESS')
            """)
    List<CallJpaEntity> findOpenBySession(@Param("establishmentId") UUID establishmentId,
                                          @Param("sessionId") UUID sessionId);

    @Query("""
            SELECT c FROM CallJpaEntity c
             WHERE c.establishmentId = :establishmentId AND c.tableId = :tableId AND c.type = :type
               AND c.status IN ('PENDING', 'IN_PROGRESS')
            """)
    Optional<CallJpaEntity> findOpen(@Param("establishmentId") UUID establishmentId, @Param("tableId") UUID tableId,
                                     @Param("type") String type);

    /**
     * O começo comum das consultas com a mesa, o setor, quem aceitou (LEFT: pode não haver ninguém)
     * e como a sessão que chamou provou a presença.
     */
    String VIEW = """
            SELECT c.id AS id, c.establishmentId AS establishmentId, c.tableId AS tableId, c.sessionId AS sessionId,
                   c.type AS type, c.paymentMethod AS paymentMethod, c.status AS status, c.createdAt AS createdAt,
                   c.acceptedAt AS acceptedAt, c.acceptedBy AS acceptedBy, c.doneAt AS doneAt,
                   c.canceledAt AS canceledAt, t.label AS tableLabel, s.name AS sectorName, u.name AS acceptedByName,
                   ts.presenceMethod AS presenceMethod
              FROM CallJpaEntity c
              JOIN DiningTableJpaEntity t ON t.id = c.tableId
              JOIN SectorJpaEntity s ON s.id = t.sectorId
              JOIN TableSessionJpaEntity ts ON ts.id = c.sessionId
              LEFT JOIN UserJpaEntity u ON u.id = c.acceptedBy
            """;

    @Query(VIEW + " WHERE c.id = :id AND c.establishmentId = :establishmentId")
    Optional<CallRow> findView(@Param("id") UUID id, @Param("establishmentId") UUID establishmentId);

    @Query(VIEW + """
             WHERE c.establishmentId = :establishmentId AND c.tableId = :tableId
               AND c.status IN ('PENDING', 'IN_PROGRESS')
             ORDER BY c.createdAt, c.id
            """)
    List<CallRow> findOpenByTable(@Param("establishmentId") UUID establishmentId, @Param("tableId") UUID tableId);

    @Query(value = VIEW + """
             WHERE c.establishmentId = :establishmentId AND c.status IN ('PENDING', 'IN_PROGRESS')
             ORDER BY c.createdAt, c.id
            """,
            countQuery = """
            SELECT count(c) FROM CallJpaEntity c
             WHERE c.establishmentId = :establishmentId AND c.status IN ('PENDING', 'IN_PROGRESS')
            """)
    Page<CallRow> findQueue(@Param("establishmentId") UUID establishmentId, Pageable pageable);
}
