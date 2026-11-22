package br.com.cardapio.adapter.out.persistence.rodizio;

import br.com.cardapio.application.port.out.rodizio.RodizioCardHistoryPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.rodizio.model.TabRodizioId;
import br.com.cardapio.domain.rodizio.model.TabRodizioStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/** As viradas do cartão da passagem, em SQL parametrizado. */
@Component
class RodizioCardHistoryPersistenceAdapter implements RodizioCardHistoryPort {

    private final JdbcClient jdbc;

    RodizioCardHistoryPersistenceAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void record(EstablishmentId establishmentId, TabRodizioId tabRodizioId, TabRodizioStatus status,
                       Instant at) {
        jdbc.sql("""
                        INSERT INTO rodizio_card_change (id, establishment_id, tab_rodizio_id, status, changed_at)
                        VALUES (:id, :establishmentId, :tabRodizioId, :status, :at)
                        """)
                .param("id", UUID.randomUUID())
                .param("establishmentId", establishmentId.value())
                .param("tabRodizioId", tabRodizioId.value())
                .param("status", status.name())
                .param("at", OffsetDateTime.ofInstant(at, ZoneOffset.UTC))
                .update();
    }
}
