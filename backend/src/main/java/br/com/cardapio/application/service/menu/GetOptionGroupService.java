package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.in.menu.OptionGroupSummary;
import br.com.cardapio.application.port.in.menu.GetOptionGroupUseCase;
import br.com.cardapio.application.port.out.menu.OptionGroupPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.OptionGroupId;
import org.springframework.transaction.annotation.Transactional;

public class GetOptionGroupService implements GetOptionGroupUseCase {

    private final OptionGroupPort groups;

    public GetOptionGroupService(OptionGroupPort groups) {
        this.groups = groups;
    }

    @Override
    @Transactional(readOnly = true)
    public OptionGroupSummary get(EstablishmentId establishmentId, OptionGroupId id) {
        return OptionGroupSummary.from(OptionLookups.activeGroup(groups, establishmentId, id));
    }
}
