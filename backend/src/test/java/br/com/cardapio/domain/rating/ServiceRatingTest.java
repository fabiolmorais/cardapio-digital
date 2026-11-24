package br.com.cardapio.domain.rating;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.rating.exception.RatingStateException;
import br.com.cardapio.domain.rating.model.RatingTag;
import br.com.cardapio.domain.rating.model.ServiceRating;
import br.com.cardapio.domain.rating.service.RatingPolicy;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.shared.model.TokenHash;
import br.com.cardapio.domain.tablesession.model.PresenceMethod;
import br.com.cardapio.domain.tablesession.model.TableSession;
import br.com.cardapio.domain.tablesession.model.TableSessionId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** A avaliação do atendimento (RF40) e quando a mesa pode avaliar. */
class ServiceRatingTest {

    private static final Instant NOW = Instant.parse("2026-11-24T23:00:00Z");
    private static final EstablishmentId PLACE = EstablishmentId.newId();
    private static final DiningTableId TABLE = DiningTableId.newId();

    private static ServiceRating rated(int stars, String comment) {
        return ServiceRating.rate(PLACE, TableSessionId.newId(), TABLE, null, null, stars,
                Set.of(RatingTag.FOOD, RatingTag.SPEED), comment, NOW);
    }

    @Test
    void starsGoFromOneToFiveAndTheCommentIsShort() {
        assertThatThrownBy(() -> rated(0, null)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> rated(6, null)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> rated(5, "x".repeat(281))).isInstanceOf(InvalidValueException.class);
        assertThat(rated(5, "   ").comment()).isNull();
        assertThat(rated(5, "  Ótimo!  ").comment()).isEqualTo("Ótimo!");
        assertThat(rated(3, null).tags()).containsExactlyInAnyOrder(RatingTag.FOOD, RatingTag.SPEED);
    }

    @Test
    void lowWarnsTheManagerAndHighInvitesToGoogle() {
        assertThat(rated(2, null).isLow()).isTrue();
        assertThat(rated(3, null).isLow()).isFalse();
        assertThat(rated(3, null).invitesToGoogle()).isFalse();
        assertThat(rated(4, null).invitesToGoogle()).isTrue();
    }

    @Test
    void theGoogleClickCountsOnceAndOnlyWithTheInvite() {
        ServiceRating five = rated(5, null);
        ServiceRating clicked = five.clickGoogle(NOW.plusSeconds(10));

        assertThat(clicked.googleClickedAt()).isEqualTo(NOW.plusSeconds(10));
        assertThat(clicked.clickGoogle(NOW.plusSeconds(60))).isSameAs(clicked);
        assertThatThrownBy(() -> rated(3, null).clickGoogle(NOW)).isInstanceOf(RatingStateException.class);
    }

    @Test
    void onlyALowRatingIsResolved() {
        UserId manager = UserId.newId();
        ServiceRating resolved = rated(1, null).resolve(manager, NOW);

        assertThat(resolved.resolvedBy()).isEqualTo(manager);
        assertThat(resolved.resolve(UserId.newId(), NOW.plusSeconds(5))).isSameAs(resolved);
        assertThatThrownBy(() -> rated(4, null).resolve(manager, NOW)).isInstanceOf(RatingStateException.class);
    }

    private static TableSession phone(boolean present) {
        TableSession open = TableSession.open(PLACE, TABLE, TokenHash.of("abc"), NOW.minus(Duration.ofHours(1)));
        return present ? open.confirmPresence(PresenceMethod.WIFI, NOW.minus(Duration.ofHours(1))) : open;
    }

    private static TableSession ended(TableSession s, Instant at) {
        return new TableSession(s.id(), s.establishmentId(), s.tableId(), s.tokenHash(), s.createdAt(),
                s.lastSeenAt(), s.expiresAt(), at, s.presenceMethod(), s.presenceConfirmedAt(), s.unlockedBy(),
                s.idleLimit(), s.blockedAt(), s.blockedBy());
    }

    @Test
    void thePhoneRatesAfterTheBillOrUpToThirtyMinutesAfterTheTabCloses() {
        TableSession present = phone(true);

        assertThat(RatingPolicy.canRate(present, false, false, NOW)).isFalse();
        assertThat(RatingPolicy.canRate(present, true, false, NOW)).isTrue();
        assertThat(RatingPolicy.canRate(phone(false), true, false, NOW)).isFalse();

        TableSession closed = ended(present, NOW.minus(Duration.ofMinutes(10)));
        assertThat(RatingPolicy.canRate(closed, true, true, NOW)).isTrue();
        assertThat(RatingPolicy.canRate(closed, true, false, NOW)).isFalse();
        assertThat(RatingPolicy.canRate(closed, true, true, NOW.plus(Duration.ofMinutes(21)))).isFalse();
    }
}
