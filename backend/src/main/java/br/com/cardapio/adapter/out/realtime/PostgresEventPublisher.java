package br.com.cardapio.adapter.out.realtime;

import br.com.cardapio.application.port.out.realtime.EventPublisherPort;
import br.com.cardapio.domain.shared.event.DomainEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Publica com o NOTIFY do Postgres. O JdbcTemplate usa a mesma conexão da transação do caso de
 * uso, e o Postgres só entrega o NOTIFY quando ela é confirmada: se der rollback, o aviso some
 * junto. É por isso que o tempo real não precisa de uma fila à parte.
 */
@Component
class PostgresEventPublisher implements EventPublisherPort {

    private final JdbcTemplate jdbc;
    private final RealtimeMessages messages;

    PostgresEventPublisher(JdbcTemplate jdbc, RealtimeMessages messages) {
        this.jdbc = jdbc;
        this.messages = messages;
    }

    @Override
    public void publish(DomainEvent event) {
        // pg_notify(canal, texto) é a forma de função do NOTIFY: aceita parâmetros (sem montar SQL com texto).
        jdbc.query("SELECT pg_notify(?, ?)", rs -> null, RealtimeMessages.CHANNEL, messages.toJson(event));
    }
}
