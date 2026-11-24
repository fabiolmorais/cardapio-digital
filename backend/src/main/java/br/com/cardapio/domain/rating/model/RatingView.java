package br.com.cardapio.domain.rating.model;

/** A avaliação com a mesa e o nome de quem atendeu, para a tela. */
public record RatingView(ServiceRating rating, String tableLabel, String waiterName) {
}
