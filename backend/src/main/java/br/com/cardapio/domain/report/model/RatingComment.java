package br.com.cardapio.domain.report.model;

import java.time.Instant;
import java.util.List;

/** Uma avaliação com comentário, para o histórico do relatório (só dono e gerente). */
public record RatingComment(String tableLabel, String waiterName, int stars, List<String> tags, String comment,
                            Instant createdAt) {

    public RatingComment {
        tags = List.copyOf(tags);
    }
}
