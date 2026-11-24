package br.com.cardapio.domain.menu.model;

/** O que o celular fez com o prato (REL-27, 28, 29). */
public enum MenuEventKind {
    /** Abriu a ficha do prato. */
    PRODUCT_VIEW,
    /** O reel do prato ficou 2 segundos na tela. */
    REEL_VIEW,
    /** Pôs o prato no carrinho. */
    CART_ADD
}
