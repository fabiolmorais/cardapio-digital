package br.com.cardapio.domain.floor.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SectorTest {

    private final EstablishmentId sakura = EstablishmentId.newId();

    @Test
    void createTrimsTheNameAndStartsActive() {
        Sector deck = Sector.create(sakura, "  Deck ", 1);

        assertThat(deck.name()).isEqualTo("Deck");
        assertThat(deck.position()).isEqualTo(1);
        assertThat(deck.active()).isTrue();
    }

    @Test
    void renameAndDeactivateKeepTheRest() {
        Sector deck = Sector.create(sakura, "Deck", 1);

        Sector varanda = deck.rename("Varanda").deactivate();

        assertThat(varanda.id()).isEqualTo(deck.id());
        assertThat(varanda.name()).isEqualTo("Varanda");
        assertThat(varanda.position()).isEqualTo(1);
        assertThat(varanda.active()).isFalse();
    }

    @Test
    void needsAShortName() {
        assertThatThrownBy(() -> Sector.create(sakura, " ", 0)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> Sector.create(sakura, "x".repeat(41), 0)).isInstanceOf(InvalidValueException.class);
    }
}
