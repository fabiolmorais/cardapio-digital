package br.com.cardapio.application.port.in.menu;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.OptionGroupId;

public interface GetOptionGroupUseCase {

    OptionGroupSummary get(EstablishmentId establishmentId, OptionGroupId id);
}
