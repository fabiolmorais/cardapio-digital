package br.com.cardapio.adapter.out.persistence.push;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface PushSubscriptionJpaRepository extends JpaRepository<PushSubscriptionJpaEntity, UUID> {

    /**
     * Apaga pelo endpoint, de qualquer restaurante. Em SQL nativo de propósito: o filtro do
     * Hibernate (por restaurante) não vale aqui, e o navegador que se inscreve de novo, com outra
     * pessoa ou noutro restaurante, deixa de mandar para a inscrição antiga.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(nativeQuery = true, value = "DELETE FROM push_subscription WHERE endpoint = :endpoint")
    int deleteByEndpointEverywhere(@Param("endpoint") String endpoint);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            DELETE FROM PushSubscriptionJpaEntity p
             WHERE p.establishmentId = :establishmentId AND p.userId = :userId AND p.endpoint = :endpoint
            """)
    int deleteOwn(@Param("establishmentId") UUID establishmentId, @Param("userId") UUID userId,
                  @Param("endpoint") String endpoint);

    /** Os navegadores de quem está ativo na equipe, com estes papéis (o vínculo é conferido agora). */
    @Query("""
            SELECT p FROM PushSubscriptionJpaEntity p, MembershipJpaEntity m
             WHERE p.establishmentId = :establishmentId
               AND m.userId = p.userId AND m.establishmentId = p.establishmentId
               AND m.active = true AND m.role IN :roles
            """)
    List<PushSubscriptionJpaEntity> findForRoles(@Param("establishmentId") UUID establishmentId,
                                                 @Param("roles") List<String> roles);
}
