package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.menu.model.MenuCategoryId;
import br.com.cardapio.domain.shared.model.Money;

/** Uma categoria do cardápio no período: unidades vendidas e faturamento dos itens (REL-16). */
public record CategorySales(MenuCategoryId categoryId, String name, int quantity, Money revenue) {
}
