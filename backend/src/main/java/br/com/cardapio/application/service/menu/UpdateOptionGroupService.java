package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.in.menu.OptionGroupSummary;
import br.com.cardapio.application.port.in.menu.OptionGroupForm;
import br.com.cardapio.application.port.in.menu.UpdateOptionGroupUseCase;
import br.com.cardapio.application.port.out.menu.OptionGroupPort;
import br.com.cardapio.application.port.out.menu.OptionGroupWithItems;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.OptionGroup;
import br.com.cardapio.domain.menu.model.OptionGroupId;
import org.springframework.transaction.annotation.Transactional;

/**
 * Salva o grupo e as opções dele. Vale para todos os produtos que usam o grupo; os pedidos já
 * feitos não mudam (eles copiaram os nomes e os preços).
 */
public class UpdateOptionGroupService implements UpdateOptionGroupUseCase {

    private final OptionGroupPort groups;

    public UpdateOptionGroupService(OptionGroupPort groups) {
        this.groups = groups;
    }

    @Override
    @Transactional
    public OptionGroupSummary update(EstablishmentId establishmentId, OptionGroupId id, OptionGroupForm form) {
        OptionGroupWithItems current = OptionLookups.activeGroup(groups, establishmentId, id);
        OptionGroup edited = current.group().edit(form.name(), form.title(), form.rules());
        groups.save(edited, OptionGroupItems.from(edited, form, current.items()));
        return OptionGroupSummary.from(OptionLookups.activeGroup(groups, establishmentId, id));
    }
}
