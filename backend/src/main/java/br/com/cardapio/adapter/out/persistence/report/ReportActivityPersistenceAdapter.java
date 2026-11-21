package br.com.cardapio.adapter.out.persistence.report;

import br.com.cardapio.application.port.out.report.ReportActivityPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * As consultas dos relatórios são SQL nativo, só leitura e sempre parametrizado (ADR 0013). O filtro
 * do Hibernate não alcança SQL nativo: toda consulta filtra por {@code establishment_id}, e o teste
 * de integração com dois restaurantes prova que um não vê o outro.
 */
@Component
class ReportActivityPersistenceAdapter implements ReportActivityPort {

    private final JdbcClient jdbc;

    ReportActivityPersistenceAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Instant> firstClosedTabAt(EstablishmentId establishmentId) {
        // Usa o índice tab_closed_idx (establishment_id, closed_at) das comandas fechadas.
        return jdbc.sql("""
                        SELECT min(closed_at) FROM tab
                        WHERE establishment_id = :establishmentId AND status = 'CLOSED'
                        """)
                .param("establishmentId", establishmentId.value())
                .query(OffsetDateTime.class)
                .list().stream()
                .filter(Objects::nonNull) // min() de nenhuma linha é uma linha com null
                .findFirst()
                .map(OffsetDateTime::toInstant);
    }
}
