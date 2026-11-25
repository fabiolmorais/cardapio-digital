package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.report.service.RatingAnalysis;

import java.util.List;
import java.util.Objects;

/**
 * Os números de uma unidade no período (REL-80), cada um contado no fuso da própria unidade.
 *
 * @param current      as comandas fechadas no período
 * @param compared     as do período anterior, do mesmo tamanho
 * @param prep         da chegada à cozinha até pronto, em média
 * @param callResponse do chamado até o garçom aceitar, em média
 * @param ratings      quantas avaliações e a soma das estrelas
 * @param series       a evolução no período
 */
public record UnitPerformance(EstablishmentId id, String name, ReportTotals current, ReportTotals compared,
                              TimeAverage prep, TimeAverage callResponse, RatingStats.StarTotal ratings,
                              List<UnitPoint> series) {

    public UnitPerformance {
        Objects.requireNonNull(id);
        Objects.requireNonNull(name);
        Objects.requireNonNull(current);
        Objects.requireNonNull(compared);
        Objects.requireNonNull(prep);
        Objects.requireNonNull(callResponse);
        Objects.requireNonNull(ratings);
        series = List.copyOf(series);
    }

    /** A nota mostrada: a média com uma casa, ou null com poucas avaliações (como a nota do garçom). */
    public Double ratingAverage() {
        return RatingAnalysis.shownAverage(ratings.count(), ratings.starSum());
    }
}
