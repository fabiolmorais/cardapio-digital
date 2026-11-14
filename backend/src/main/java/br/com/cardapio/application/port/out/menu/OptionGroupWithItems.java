package br.com.cardapio.application.port.out.menu;

import br.com.cardapio.domain.menu.model.OptionGroup;
import br.com.cardapio.domain.menu.model.OptionItem;

import java.util.List;

/**
 * Um grupo de opções com as opções ativas dele, na ordem.
 *
 * @param productCount em quantos produtos ativos o grupo está ligado
 */
public record OptionGroupWithItems(OptionGroup group, List<OptionItem> items, long productCount) {

    public OptionGroupWithItems {
        items = List.copyOf(items);
    }
}
