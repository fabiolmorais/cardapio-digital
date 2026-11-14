package br.com.cardapio.domain.menu.model;

/**
 * Como as escolhas de um grupo entram no preço do item (ADR 0010).
 */
public enum OptionPricing {

    /** Cada opção soma o seu preço (os adicionais). O padrão. */
    SUM,
    /** Cobra só a opção mais cara (a pizza meio a meio pelo sabor mais caro). */
    HIGHEST,
    /** A média das opções escolhidas, arredondada para cima no centavo. */
    AVERAGE
}
