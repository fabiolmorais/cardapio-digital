package br.com.cardapio.domain.menu.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;

import java.util.Objects;

/**
 * Um evento do celular no cardápio (REL-27, 28, 29), anônimo: o prato e o que aconteceu com ele.
 *
 * @param quantity quantos foram para o carrinho (1 nos outros eventos)
 */
public record MenuEvent(MenuEventKind kind, ProductId productId, int quantity) {

    /** Quantos eventos cabem num envio do celular. */
    public static final int MAX_BATCH = 50;

    /** Quantos eventos uma sessão grava, no máximo: passou disso, o resto é ignorado (não é uso normal). */
    public static final int MAX_PER_SESSION = 2000;

    /** Quantos dias os eventos ficam: 13 meses, um a mais que o histórico do Business. */
    public static final int RETENTION_DAYS = 395;

    public MenuEvent {
        Objects.requireNonNull(kind, "tipo é obrigatório");
        Objects.requireNonNull(productId, "prato é obrigatório");
        if (kind == MenuEventKind.CART_ADD ? quantity < 1 || quantity > 20 : quantity != 1) {
            throw new InvalidValueException("Quantidade inválida no evento do cardápio");
        }
    }
}
