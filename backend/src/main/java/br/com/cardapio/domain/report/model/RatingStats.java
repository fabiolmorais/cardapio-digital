package br.com.cardapio.domain.report.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * As avaliações do atendimento num período (REL-83, 84), como o banco soma.
 *
 * @param starSum      a soma das estrelas (a média sai daqui)
 * @param byStars      quantas de cada nota, de 1 a 5 (índice 0 = 1 estrela)
 * @param lunch        as do almoço (até {@code RatingAnalysis.LUNCH_UNTIL}h, no fuso do restaurante)
 * @param dinner       as do jantar
 * @param praisedTags  os atalhos marcados com nota 4 ou 5, por atalho (o nome do enum)
 * @param improveTags  os atalhos marcados com nota 1 a 3
 * @param closedTabs   as comandas fechadas no período (a base de "quantas comandas avaliaram")
 */
public record RatingStats(int count, long starSum, int low, int high, int googleClicks, List<Integer> byStars,
                          StarTotal lunch, StarTotal dinner, Map<String, Integer> praisedTags,
                          Map<String, Integer> improveTags, int closedTabs) {

    public RatingStats {
        byStars = List.copyOf(byStars);
        praisedTags = Map.copyOf(praisedTags);
        improveTags = Map.copyOf(improveTags);
        Objects.requireNonNull(lunch);
        Objects.requireNonNull(dinner);
    }

    /** Quantas avaliações e a soma das estrelas de um grupo. */
    public record StarTotal(int count, long starSum) {

        public static final StarTotal NONE = new StarTotal(0, 0);

        /** A média, com uma casa; null sem avaliações. */
        public Double average() {
            return count == 0 ? null : Math.round(starSum * 10.0 / count) / 10.0;
        }
    }

    public Double average() {
        return new StarTotal(count, starSum).average();
    }
}
