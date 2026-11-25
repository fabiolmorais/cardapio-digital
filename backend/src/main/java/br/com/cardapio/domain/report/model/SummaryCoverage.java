package br.com.cardapio.domain.report.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Como um intervalo se divide entre o resumo diário e as consultas diretas (ADR 0014).
 *
 * @param from  o primeiro dia lido do resumo; vazio quando o resumo não cobre nenhum dia inteiro
 * @param until o dia seguinte ao último lido do resumo
 * @param live  o que fica para as consultas diretas: as pontas que não são dias inteiros e os dias
 *              que o resumo ainda não tem
 */
public record SummaryCoverage(Optional<LocalDate> from, Optional<LocalDate> until, List<TimeSpan> live) {

    public SummaryCoverage {
        Objects.requireNonNull(from);
        Objects.requireNonNull(until);
        live = List.copyOf(live);
        if (from.isPresent() != until.isPresent()) {
            throw new IllegalArgumentException("o resumo precisa de início e fim");
        }
    }

    public boolean usesSummary() {
        return from.isPresent();
    }
}
