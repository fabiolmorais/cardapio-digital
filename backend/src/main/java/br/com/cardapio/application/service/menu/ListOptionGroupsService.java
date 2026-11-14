package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.in.menu.OptionGroupSummary;
import br.com.cardapio.application.port.in.menu.ListOptionGroupsUseCase;
import br.com.cardapio.application.port.out.menu.OptionGroupPort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import org.springframework.transaction.annotation.Transactional;

public class ListOptionGroupsService implements ListOptionGroupsUseCase {

    private final OptionGroupPort groups;

    public ListOptionGroupsService(OptionGroupPort groups) {
        this.groups = groups;
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPage<OptionGroupSummary> list(EstablishmentId establishmentId, PageQuery page) {
        return groups.findActive(establishmentId, page).map(OptionGroupSummary::from);
    }
}
