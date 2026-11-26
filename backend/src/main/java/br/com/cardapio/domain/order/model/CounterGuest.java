package br.com.cardapio.domain.order.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;

/**
 * Quem está na comanda do balcão (ADR 0015): como a equipe chama o cliente ("João", "camisa azul")
 * e, se a casa usa cartões numerados, o número do cartão.
 *
 * @param name obrigatório, até {@link #NAME_MAX} letras
 * @param card opcional (null), até {@link #CARD_MAX} letras ou números
 */
public record CounterGuest(String name, String card) {

    public static final int NAME_MAX = 40;
    public static final int CARD_MAX = 10;

    public CounterGuest {
        if (name == null || name.isBlank()) {
            throw new InvalidValueException("Diga como chamar o cliente, como \"João\" ou \"camisa azul\"");
        }
        name = name.strip().replaceAll("\\s+", " ");
        if (name.length() > NAME_MAX) {
            throw new InvalidValueException("O nome vai até " + NAME_MAX + " letras");
        }
        if (card != null && card.isBlank()) {
            card = null;
        }
        if (card != null) {
            card = card.strip();
            if (card.length() > CARD_MAX || !card.matches("[\\p{L}\\p{N}-]+")) {
                throw new InvalidValueException("O cartão vai até " + CARD_MAX + " letras ou números, sem espaço");
            }
        }
    }
}
