package br.com.cardapio.application.port.in.menu;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.OptionGroupId;

public interface GetOptionGroupPricesUseCase {

    /** A grade de preços do grupo: as colunas dos produtos ativos que o usam e os preços preenchidos. */
    OptionGroupPrices get(EstablishmentId establishmentId, OptionGroupId groupId);
}
