package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.shared.model.Money;

/**
 * A curva ABC do cardápio (REL-15): A são os itens que, do maior faturamento para o menor, somam os
 * primeiros 80% do faturamento; B, os próximos 15%; C, o resto.
 */
public enum AbcClass {
    A, B, C;

    /**
     * A classe de um item pelo que os itens à frente dele já somam ({@code before}) no total do
     * período: o item que começa antes dos 80% é A, mesmo que passe deles.
     */
    public static AbcClass of(Money before, Money total) {
        if (total.cents() <= 0) {
            return C;
        }
        double share = (double) before.cents() / total.cents();
        return share < 0.80 ? A : share < 0.95 ? B : C;
    }
}
