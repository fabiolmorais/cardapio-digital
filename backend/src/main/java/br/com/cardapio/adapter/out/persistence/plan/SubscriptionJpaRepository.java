package br.com.cardapio.adapter.out.persistence.plan;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SubscriptionJpaRepository extends JpaRepository<SubscriptionJpaEntity, UUID> {

    @Query(nativeQuery = true, value = """
            SELECT s.* FROM subscription s
            JOIN establishment e ON e.organization_id = s.organization_id
            WHERE e.id = :establishmentId
            """)
    Optional<SubscriptionJpaEntity> findByEstablishment(@Param("establishmentId") UUID establishmentId);

    /** A mesma busca, travando a linha da assinatura até o fim da transação. */
    @Query(nativeQuery = true, value = """
            SELECT s.* FROM subscription s
            JOIN establishment e ON e.organization_id = s.organization_id
            WHERE e.id = :establishmentId
            FOR UPDATE OF s
            """)
    Optional<SubscriptionJpaEntity> lockByEstablishment(@Param("establishmentId") UUID establishmentId);

    /** O que o plano conta, numa consulta só: mesas, pessoas da equipe e setores ativos. */
    @Query(nativeQuery = true, value = """
            SELECT (SELECT count(*) FROM dining_table WHERE establishment_id = :establishmentId AND active) AS tables,
                   (SELECT count(*) FROM membership WHERE establishment_id = :establishmentId AND active) AS users,
                   (SELECT count(*) FROM sector WHERE establishment_id = :establishmentId AND active) AS sectors
            """)
    UsageRow usage(@Param("establishmentId") UUID establishmentId);

    /** Os testes que acabam antes de {@code endsBefore} e ainda não passaram da tolerância. */
    @Query(nativeQuery = true, value = """
            SELECT * FROM subscription
            WHERE status = 'TRIALING' AND trial_ends_at < :endsBefore AND trial_ends_at > :graceStartedBefore
            ORDER BY trial_ends_at
            """)
    List<SubscriptionJpaEntity> findTrialsEndingBetween(@Param("graceStartedBefore") Instant graceStartedBefore,
                                                        @Param("endsBefore") Instant endsBefore);

    /** Para quem vai o aviso: o e-mail da empresa, e o nome e o fuso do (primeiro) restaurante dela. */
    @Query(nativeQuery = true, value = """
            SELECT DISTINCT ON (o.id) CAST(o.id AS text) AS organizationId, o.email AS email,
                   e.name AS establishmentName, e.time_zone AS timeZone
            FROM organization o
            JOIN establishment e ON e.organization_id = o.id
            WHERE o.id IN (:organizationIds)
            ORDER BY o.id, e.created_at
            """)
    List<ContactRow> contacts(@Param("organizationIds") Collection<UUID> organizationIds);

    /** Marca o aviso; 0 se ele já estava marcado (a chave primária barra o repetido). */
    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = """
            INSERT INTO subscription_notice (subscription_id, kind, sent_at) VALUES (:subscriptionId, :kind, :sentAt)
            ON CONFLICT DO NOTHING
            """)
    int markNotice(@Param("subscriptionId") UUID subscriptionId, @Param("kind") String kind,
                   @Param("sentAt") Instant sentAt);

    interface ContactRow {
        String getOrganizationId();

        String getEmail();

        String getEstablishmentName();

        String getTimeZone();
    }

    interface UsageRow {
        long getTables();

        long getUsers();

        long getSectors();
    }
}
