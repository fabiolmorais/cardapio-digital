package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.in.menu.OptionGroupSummary;
import br.com.cardapio.application.port.in.menu.CreateOptionGroupUseCase;
import br.com.cardapio.application.port.in.menu.OptionGroupForm;
import br.com.cardapio.application.port.out.menu.OptionGroupPort;
import br.com.cardapio.application.port.out.menu.OptionGroupWithItems;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.OptionGroup;
import br.com.cardapio.domain.menu.model.OptionItem;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class CreateOptionGroupService implements CreateOptionGroupUseCase {

    private final OptionGroupPort groups;

    public CreateOptionGroupService(OptionGroupPort groups) {
        this.groups = groups;
    }

    @Override
    @Transactional
    public OptionGroupSummary create(EstablishmentId establishmentId, OptionGroupForm form) {
        if (groups.countActive(establishmentId) >= OptionGroup.MAX_PER_ESTABLISHMENT) {
            throw new InvalidValueException("O restaurante já tem " + OptionGroup.MAX_PER_ESTABLISHMENT
                    + " grupos de opções. Desative os que não usa mais.");
        }
        OptionGroup group = OptionGroup.create(establishmentId, form.name(), form.title(), form.rules());
        List<OptionItem> items = OptionGroupItems.from(group, form, List.of());
        groups.save(group, items);
        return new OptionGroupSummary(group, items.stream().filter(OptionItem::active).toList(), 0);
    }
}
