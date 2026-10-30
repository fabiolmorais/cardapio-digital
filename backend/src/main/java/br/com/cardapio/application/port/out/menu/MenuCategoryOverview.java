package br.com.cardapio.application.port.out.menu;

import br.com.cardapio.domain.menu.model.MenuCategory;

/** A categoria e quantos produtos ativos ela tem (a tela mostra "Entradas · 8"). */
public record MenuCategoryOverview(MenuCategory category, long activeProducts) {
}
