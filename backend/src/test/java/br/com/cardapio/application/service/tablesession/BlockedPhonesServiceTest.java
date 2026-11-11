package br.com.cardapio.application.service.tablesession;

import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.shared.event.DomainEvent;
import br.com.cardapio.domain.tablesession.event.PhoneBlockChanged;
import br.com.cardapio.domain.tablesession.exception.TableSessionNotFoundException;
import br.com.cardapio.domain.tablesession.model.PresenceMethod;
import br.com.cardapio.domain.tablesession.model.TableSession;
import br.com.cardapio.support.InMemoryTableSessions;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** A lista de celulares bloqueados e o "Desbloquear". */
class BlockedPhonesServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-11T20:00:00Z");

    private final InMemoryTableSessions sessions = new InMemoryTableSessions();
    private final List<DomainEvent> published = new ArrayList<>();
    private final BlockedPhonesService service = new BlockedPhonesService(sessions, published::add,
            Clock.fixed(NOW, ZoneOffset.UTC));
    private final EstablishmentId sakura = EstablishmentId.newId();
    private final DiningTableId mesa04 = DiningTableId.newId();
    private final UserId carlos = UserId.newId();

    private TableSession blocked(String token) {
        TableSession session = sessions.givenPresent(sakura, mesa04, token, NOW);
        sessions.block(sakura, session.id(), carlos, NOW);
        return session;
    }

    @Test
    void unblockingGivesThePhoneItsButtonsBackAndTellsTheTable() {
        TableSession prankster = blocked("trote");
        assertThat(service.list(sakura, new PageQuery(0, 20)).content()).singleElement()
                .satisfies(p -> assertThat(p.sessionId()).isEqualTo(prankster.id()));

        service.unblock(sakura, prankster.id());

        TableSession after = sessions.byToken("trote");
        assertThat(after.isBlocked()).isFalse();
        assertThat(after.presenceMethod()).isEqualTo(PresenceMethod.WIFI); // a presença de antes continua
        assertThat(service.list(sakura, new PageQuery(0, 20)).content()).isEmpty();
        assertThat(published).singleElement().isInstanceOfSatisfying(PhoneBlockChanged.class, e -> {
            assertThat(e.blocked()).isFalse();
            assertThat(e.tableId()).isEqualTo(mesa04);
        });
    }

    @Test
    void unblockingAPhoneThatIsNotBlockedChangesNothing() {
        TableSession fine = sessions.givenPresent(sakura, mesa04, "normal", NOW);

        service.unblock(sakura, fine.id());

        assertThat(published).isEmpty();
    }

    @Test
    void anotherRestaurantNeitherSeesNorUnblocks() {
        TableSession prankster = blocked("trote");
        EstablishmentId other = EstablishmentId.newId();

        assertThat(service.list(other, new PageQuery(0, 20)).content()).isEmpty();
        assertThatThrownBy(() -> service.unblock(other, prankster.id()))
                .isInstanceOf(TableSessionNotFoundException.class);
        assertThat(sessions.byToken("trote").isBlocked()).isTrue();
    }
}
