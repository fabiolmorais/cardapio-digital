package br.com.cardapio.domain.floor.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TableSituationTest {

    @Test
    void theOneThatNeedsMoreAttentionWins() {
        assertThat(TableSituation.of(true, false, false, false)).isEqualTo(TableSituation.FREE);
        assertThat(TableSituation.of(true, true, false, false)).isEqualTo(TableSituation.OCCUPIED);
        assertThat(TableSituation.of(true, true, false, true)).isEqualTo(TableSituation.RODIZIO);
        assertThat(TableSituation.of(true, true, true, true)).isEqualTo(TableSituation.BILL_REQUESTED);
        assertThat(TableSituation.of(false, true, true, true)).isEqualTo(TableSituation.INACTIVE);
    }
}
