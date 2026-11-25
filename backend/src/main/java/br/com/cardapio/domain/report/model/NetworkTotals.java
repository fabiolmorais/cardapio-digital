package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.report.service.RatingAnalysis;

import java.util.Objects;

/** A rede inteira no período: a soma das unidades e as médias pesadas pelo tamanho de cada uma. */
public record NetworkTotals(ReportTotals current, ReportTotals compared, TimeAverage prep, TimeAverage callResponse,
                            RatingStats.StarTotal ratings) {

    public NetworkTotals {
        Objects.requireNonNull(current);
        Objects.requireNonNull(compared);
        Objects.requireNonNull(prep);
        Objects.requireNonNull(callResponse);
        Objects.requireNonNull(ratings);
    }

    public Double ratingAverage() {
        return RatingAnalysis.shownAverage(ratings.count(), ratings.starSum());
    }
}
