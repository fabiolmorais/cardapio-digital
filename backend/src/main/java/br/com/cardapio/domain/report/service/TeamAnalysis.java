package br.com.cardapio.domain.report.service;

import br.com.cardapio.domain.report.model.ShiftLoad;
import br.com.cardapio.domain.report.model.TableCalls;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** As regras do relatório de atendimento e equipe (REL-44, 53). */
public final class TeamAnalysis {

    /** Uma mesa só entra em "Mesas que mais chamam" com 3 comandas ou mais no período. */
    public static final int MIN_TABS = 3;

    /** Quantas mesas e quantas horas aparecem. */
    public static final int TOP_TABLES = 2;
    public static final int TOP_LOADS = 2;

    private TeamAnalysis() {
    }

    /** As mesas que mais chamam por comanda (com {@link #MIN_TABS} comandas ou mais; empate: o nome). */
    public static List<TableCalls> topTables(Collection<TableCalls> tables, int limit) {
        return tables.stream()
                .filter(t -> t.tabs() >= MIN_TABS)
                .sorted(Comparator.comparingDouble(TableCalls::perTab).reversed().thenComparing(TableCalls::label))
                .limit(limit)
                .toList();
    }

    /** A média do restaurante: todos os chamados das mesas pelas comandas fechadas; vazio sem comandas. */
    public static Optional<Double> average(Collection<TableCalls> tables) {
        int tabs = tables.stream().mapToInt(TableCalls::tabs).sum();
        int calls = tables.stream().mapToInt(TableCalls::calls).sum();
        return tabs == 0 ? Optional.empty() : Optional.of((double) calls / tabs);
    }

    /** As horas com mais chamados por pessoa do salão no turno (empate: mais chamados, depois a primeira). */
    public static List<ShiftLoad> busiest(Collection<ShiftLoad> loads, int limit) {
        return loads.stream()
                .filter(l -> l.waiters() > 0)
                .sorted(Comparator.comparingDouble(ShiftLoad::callsPerWaiter).reversed()
                        .thenComparing(Comparator.comparingInt(ShiftLoad::calls).reversed())
                        .thenComparing(ShiftLoad::day)
                        .thenComparingInt(ShiftLoad::hour))
                .limit(limit)
                .toList();
    }
}
