package br.com.cardapio.adapter.out.persistence.shared;

import br.com.cardapio.domain.account.model.EstablishmentId;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Liga o filtro por restaurante na sessão do Hibernate da transação atual.
 *
 * É a segunda barreira: as consultas já recebem o establishmentId explicitamente, e o filtro
 * garante o mesmo corte mesmo se alguém esquecer o "WHERE establishment_id" numa consulta nova.
 */
@Component
public class EstablishmentFilter {
    public static final String NAME = "establishmentFilter";
    public static final String PARAMETER = "establishmentId";

    private final EntityManager entityManager;

    public EstablishmentFilter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void enable(EstablishmentId establishmentId) {
        // Fora de uma transação, cada consulta abre uma sessão nova e o filtro ligado aqui se perderia.
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("O filtro por restaurante precisa de uma transação ativa");
        }
        entityManager.unwrap(Session.class)
                .enableFilter(NAME)
                .setParameter(PARAMETER, establishmentId.value());
    }
}
