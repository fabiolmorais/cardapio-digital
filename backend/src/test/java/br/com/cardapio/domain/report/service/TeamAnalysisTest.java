package br.com.cardapio.domain.report.service;

import br.com.cardapio.domain.report.model.ShiftLoad;
import br.com.cardapio.domain.report.model.TableCalls;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TeamAnalysisTest {

    @Test
    void theTablesThatCallTheMostPerTabNeedThreeTabs() {
        var tables = List.of(new TableCalls("D3", "Deck", 41, 10), new TableCalls("12", "Salão", 32, 10),
                new TableCalls("07", "Salão", 9, 2), new TableCalls("01", "Salão", 8, 10));

        assertThat(TeamAnalysis.topTables(tables, 2)).extracting(TableCalls::label).containsExactly("D3", "12");
        // 90 chamados em 32 comandas.
        assertThat(TeamAnalysis.average(tables)).hasValueSatisfying(v -> assertThat(v).isEqualTo(90.0 / 32));
        assertThat(TeamAnalysis.average(List.of(new TableCalls("01", "Salão", 3, 0)))).isEmpty();
    }

    @Test
    void theBusiestHoursHaveTheMostCallsPerWaiterOnShift() {
        LocalDate saturday = LocalDate.of(2026, 11, 14);
        var loads = List.of(new ShiftLoad(saturday, 21, 76, 2), new ShiftLoad(saturday.minusDays(1), 21, 66, 3),
                new ShiftLoad(saturday, 13, 40, 0), new ShiftLoad(saturday, 20, 50, 2));

        assertThat(TeamAnalysis.busiest(loads, 2)).extracting(ShiftLoad::hour, ShiftLoad::waiters)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(21, 2), org.assertj.core.groups.Tuple.tuple(20, 2));
    }
}
