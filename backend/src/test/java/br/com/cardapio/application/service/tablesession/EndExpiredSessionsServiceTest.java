package br.com.cardapio.application.service.tablesession;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.tablesession.model.TableSession;
import br.com.cardapio.support.InMemoryTableSessions;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class EndExpiredSessionsServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-02T20:00:00Z");

    @Test
    void endsOnlyTheExpiredAndRecordsWhenTheyExpired() {
        InMemoryTableSessions sessions = new InMemoryTableSessions();
        EstablishmentId sakura = EstablishmentId.newId();
        TableSession old = sessions.given(sakura, DiningTableId.newId(), "antiga", NOW.minus(TableSession.IDLE_LIMIT));
        sessions.given(sakura, DiningTableId.newId(), "nova", NOW);

        int ended = new EndExpiredSessionsService(sessions, Clock.fixed(NOW, ZoneOffset.UTC)).endExpired();

        assertThat(ended).isEqualTo(1);
        assertThat(sessions.byToken("antiga").endedAt()).isEqualTo(old.expiresAt());
        assertThat(sessions.byToken("nova").endedAt()).isNull();
    }
}
