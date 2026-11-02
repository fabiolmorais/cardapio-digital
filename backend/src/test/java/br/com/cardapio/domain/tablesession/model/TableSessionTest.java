package br.com.cardapio.domain.tablesession.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.shared.model.TokenHash;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TableSessionTest {

    private static final Instant NOW = Instant.parse("2026-11-02T20:00:00Z");
    private final DiningTableId mesa04 = DiningTableId.newId();
    private final TableSession session = TableSession.open(EstablishmentId.newId(), mesa04, TokenHash.of("abc"), NOW);

    @Test
    void itVanishesAfterTwoHoursWithoutUse() {
        assertThat(session.isActive(NOW.plus(Duration.ofMinutes(119)))).isTrue();
        assertThat(session.isActive(NOW.plus(TableSession.IDLE_LIMIT))).isFalse();
    }

    @Test
    void eachUsePushesTheDeadline() {
        Instant later = NOW.plus(Duration.ofMinutes(90));

        TableSession touched = session.touch(later);

        assertThat(touched.isActive(NOW.plus(Duration.ofMinutes(150)))).isTrue();
        assertThat(touched.lastSeenAt()).isEqualTo(later);
        assertThat(touched.createdAt()).isEqualTo(NOW);
        assertThat(touched.id()).isEqualTo(session.id());
    }

    @Test
    void itBelongsToOneTable() {
        assertThat(session.isFor(mesa04)).isTrue();
        assertThat(session.isFor(DiningTableId.newId())).isFalse();
    }

    @Test
    void theHashIsSha256AndNeverTheToken() {
        TokenHash hash = TokenHash.of("token-secreto");

        assertThat(hash.value()).hasSize(64).doesNotContain("token-secreto");
        assertThat(TokenHash.of("token-secreto")).isEqualTo(hash);
        assertThat(TokenHash.of("outro")).isNotEqualTo(hash);
    }
}
