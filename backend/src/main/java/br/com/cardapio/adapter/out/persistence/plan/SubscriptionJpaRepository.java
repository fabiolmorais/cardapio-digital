package br.com.cardapio.adapter.out.persistence.plan;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    interface UsageRow {
        long getTables();

        long getUsers();

        long getSectors();
    }
}
