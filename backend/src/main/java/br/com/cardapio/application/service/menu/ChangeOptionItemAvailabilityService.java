package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.in.menu.ChangeOptionItemAvailabilityUseCase;
import br.com.cardapio.application.port.out.menu.OptionGroupPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.exception.OptionItemNotFoundException;
import br.com.cardapio.domain.menu.model.OptionItem;
import br.com.cardapio.domain.menu.model.OptionItemId;
import org.springframework.transaction.annotation.Transactional;

/** "Acabou a borda de catupiry": a opção esgota em todos os produtos que usam o grupo. */
public class ChangeOptionItemAvailabilityService implements ChangeOptionItemAvailabilityUseCase {

    private final OptionGroupPort groups;

    public ChangeOptionItemAvailabilityService(OptionGroupPort groups) {
        this.groups = groups;
    }

    @Override
    @Transactional
    public OptionItem change(EstablishmentId establishmentId, OptionItemId id, boolean available) {
        OptionItem item = groups.findItem(establishmentId, id)
                .filter(OptionItem::active)
                .orElseThrow(OptionItemNotFoundException::new);
        OptionItem changed = item.withAvailability(available);
        groups.saveItem(changed);
        return changed;
    }
}
