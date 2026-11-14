package br.com.cardapio.application.port.in.menu;

import br.com.cardapio.application.port.out.menu.OptionGroupWithItems;
import br.com.cardapio.domain.menu.model.OptionGroup;
import br.com.cardapio.domain.menu.model.OptionItem;

import java.util.List;

/**
 * Um grupo de opções como a gestão vê: o grupo, as opções ativas na ordem e em quantos produtos ele
 * está ligado.
 */
public record OptionGroupSummary(OptionGroup group, List<OptionItem> items, long productCount) {

    public OptionGroupSummary {
        items = List.copyOf(items);
    }

    public static OptionGroupSummary from(OptionGroupWithItems found) {
        return new OptionGroupSummary(found.group(), found.items(), found.productCount());
    }
}
