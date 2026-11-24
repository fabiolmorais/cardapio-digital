package br.com.cardapio.domain.rating.model;

/**
 * O resumo de um conjunto de avaliações.
 *
 * @param average null sem avaliações
 * @param high    as notas 4 e 5 (as que viram o convite ao Google)
 */
public record RatingSummary(int count, Double average, int low, int high, int googleClicks) {
}
