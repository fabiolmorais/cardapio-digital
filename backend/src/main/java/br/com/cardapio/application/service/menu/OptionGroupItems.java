package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.in.menu.OptionGroupForm;
import br.com.cardapio.domain.menu.exception.OptionItemNotFoundException;
import br.com.cardapio.domain.menu.model.OptionGroup;
import br.com.cardapio.domain.menu.model.OptionItem;
import br.com.cardapio.domain.menu.model.OptionItemId;
import br.com.cardapio.domain.shared.exception.InvalidValueException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * As opções de um grupo a partir do formulário: as novas ganham id, as que já existiam são editadas
 * (pelo id), e as que saíram do formulário são desativadas, nunca apagadas (os pedidos apontam
 * para elas). A ordem é a do formulário.
 */
final class OptionGroupItems {

    private OptionGroupItems() {
    }

    /**
     * @param existing as opções ativas do grupo hoje (vazio num grupo novo)
     * @return todas as opções a gravar: as do formulário e as que saíram, desativadas
     */
    static List<OptionItem> from(OptionGroup group, OptionGroupForm form, List<OptionItem> existing) {
        if (form.items().isEmpty() || form.items().size() > OptionGroup.MAX_ITEMS) {
            throw new InvalidValueException("Um grupo tem de 1 a " + OptionGroup.MAX_ITEMS + " opções");
        }
        Set<String> names = new HashSet<>();
        for (OptionGroupForm.Item item : form.items()) {
            String name = item.name() == null ? "" : item.name().strip().toLowerCase(Locale.ROOT);
            if (!names.add(name)) {
                throw new InvalidValueException("Há duas opções com o nome \"" + item.name().strip() + "\"");
            }
        }
        var rules = group.rules();
        if (rules.minChoices() > form.items().size() * rules.maxPerOption()) {
            throw new InvalidValueException("O grupo pede " + rules.minChoices()
                    + " escolhas, mas não tem opções suficientes");
        }
        Map<OptionItemId, OptionItem> current = existing.stream()
                .collect(Collectors.toMap(OptionItem::id, Function.identity()));
        List<OptionItem> result = new ArrayList<>();
        Set<OptionItemId> kept = new HashSet<>();
        for (int position = 0; position < form.items().size(); position++) {
            OptionGroupForm.Item item = form.items().get(position);
            OptionItemId id;
            if (item.id() == null) {
                id = OptionItemId.newId();
            } else if (current.containsKey(item.id()) && kept.add(item.id())) {
                id = item.id();
            } else {
                throw new OptionItemNotFoundException();
            }
            result.add(new OptionItem(id, group.establishmentId(), group.id(), item.name(), item.price(),
                    item.available(), position, true));
        }
        existing.stream().filter(i -> !kept.contains(i.id())).map(OptionItem::deactivate).forEach(result::add);
        return result;
    }
}
