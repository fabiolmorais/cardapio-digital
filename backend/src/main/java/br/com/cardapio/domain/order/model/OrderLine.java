package br.com.cardapio.domain.order.model;

import br.com.cardapio.domain.menu.model.OptionSelection;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.shared.exception.InvalidValueException;

import java.util.Objects;

/**
 * Uma linha do carrinho, como o celular manda: qual produto, quantos e a observação ("sem
 * cebolinha"). O nome e o preço não vêm do celular: o back copia do cardápio na hora.
 *
 * @param note      null ou em branco se não houver observação
 * @param selection o tamanho e as opções escolhidas (ADR 0010); {@link OptionSelection#NONE} sem opções
 */
public record OrderLine(ProductId productId, int quantity, String note, OptionSelection selection) {

    public static final int MAX_QUANTITY = 20;
    public static final int MAX_NOTE = 140;

    public OrderLine {
        Objects.requireNonNull(productId);
        if (quantity < 1 || quantity > MAX_QUANTITY) {
            throw new InvalidValueException("A quantidade de cada item deve ser de 1 a " + MAX_QUANTITY);
        }
        note = note == null || note.isBlank() ? null : note.strip();
        if (note != null && note.length() > MAX_NOTE) {
            throw new InvalidValueException("A observação pode ter até " + MAX_NOTE + " caracteres");
        }
        selection = selection == null ? OptionSelection.NONE : selection;
    }

    /** O produto de sempre, sem tamanho nem opções. */
    public OrderLine(ProductId productId, int quantity, String note) {
        this(productId, quantity, note, OptionSelection.NONE);
    }
}
