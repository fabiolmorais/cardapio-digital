package br.com.cardapio.application.port.in.menu;

import br.com.cardapio.domain.account.model.EstablishmentId;

public interface CreateOptionGroupUseCase {

    OptionGroupSummary create(EstablishmentId establishmentId, OptionGroupForm form);
}
