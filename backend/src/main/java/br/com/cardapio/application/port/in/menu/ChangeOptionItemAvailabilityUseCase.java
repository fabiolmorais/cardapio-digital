package br.com.cardapio.application.port.in.menu;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.OptionItem;
import br.com.cardapio.domain.menu.model.OptionItemId;

public interface ChangeOptionItemAvailabilityUseCase {

    OptionItem change(EstablishmentId establishmentId, OptionItemId id, boolean available);
}
