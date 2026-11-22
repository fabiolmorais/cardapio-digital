package br.com.cardapio.adapter.out.persistence.order;

import br.com.cardapio.application.port.out.order.TabPeoplePort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.order.model.TabPeople;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.sql.Types;
import java.util.Objects;
import java.util.Optional;

/**
 * As pessoas na mesa, em SQL parametrizado. A coluna fica fora da entidade JPA da comanda: o
 * Hibernate não a regrava ao salvar a comanda.
 */
@Component
class TabPeoplePersistenceAdapter implements TabPeoplePort {

    private final JdbcClient jdbc;

    TabPeoplePersistenceAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<TabPeople> find(EstablishmentId establishmentId, TabId tabId) {
        return jdbc.sql("SELECT people FROM tab WHERE id = :tabId AND establishment_id = :establishmentId")
                .param("tabId", tabId.value())
                .param("establishmentId", establishmentId.value())
                .query((rs, row) -> rs.getObject("people", Integer.class))
                .list().stream()
                .filter(Objects::nonNull)
                .findFirst()
                .map(TabPeople::new);
    }

    @Override
    public void save(EstablishmentId establishmentId, TabId tabId, TabPeople people) {
        jdbc.sql("UPDATE tab SET people = :people WHERE id = :tabId AND establishment_id = :establishmentId")
                .param("people", people == null ? null : people.value(), Types.INTEGER)
                .param("tabId", tabId.value())
                .param("establishmentId", establishmentId.value())
                .update();
    }
}
