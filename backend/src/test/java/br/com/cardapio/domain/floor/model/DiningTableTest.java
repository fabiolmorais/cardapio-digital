package br.com.cardapio.domain.floor.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiningTableTest {

    private static final Instant NOW = Instant.parse("2026-10-28T12:00:00Z");
    private static final TableToken TOKEN = new TableToken("token-da-mesa-0000000001");

    private final EstablishmentId sakura = EstablishmentId.newId();
    private final SectorId salao = SectorId.newId();

    private DiningTable table(String label) {
        return DiningTable.create(sakura, salao, label, TOKEN, NOW);
    }

    @Test
    void createCleansTheLabelAndStartsActive() {
        DiningTable table = table("  Varanda   2 ");

        assertThat(table.label()).isEqualTo("Varanda 2");
        assertThat(table.active()).isTrue();
        assertThat(table.tokenIssuedAt()).isEqualTo(NOW);
    }

    @ParameterizedTest
    @ValueSource(strings = {"04", "B-12", "Varanda 2", "Área 1"})
    void acceptsLettersDigitsSpacesAndHyphens(String label) {
        assertThat(table(label).label()).isEqualTo(label);
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "Mesa/04", "<b>", "12345678901"})
    void rejectsEmptyLongOrStrangeLabels(String label) {
        assertThatThrownBy(() -> table(label)).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void rotatingTheTokenChangesOnlyTheTokenAndItsDate() {
        DiningTable table = table("04");
        TableToken newToken = new TableToken("token-da-mesa-0000000002");

        DiningTable rotated = table.rotateToken(newToken, NOW.plusSeconds(60));

        assertThat(rotated.token()).isEqualTo(newToken);
        assertThat(rotated.tokenIssuedAt()).isEqualTo(NOW.plusSeconds(60));
        assertThat(rotated.id()).isEqualTo(table.id());
        assertThat(rotated.label()).isEqualTo("04");
    }

    @Test
    void moveDeactivateAndActivate() {
        SectorId deck = SectorId.newId();

        DiningTable moved = table("04").moveTo(deck).deactivate();

        assertThat(moved.sectorId()).isEqualTo(deck);
        assertThat(moved.active()).isFalse();
        assertThat(moved.activate().active()).isTrue();
    }

    @Test
    void tokenNeedsAUrlSafeFormatAndHidesItselfFromLogs() {
        assertThatThrownBy(() -> new TableToken("curto")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TableToken("tem espaço e é longo o bastante")).isInstanceOf(IllegalArgumentException.class);
        assertThat(TOKEN.toString()).doesNotContain(TOKEN.value());
    }
}
