package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.out.menu.OptionGroupPort;
import br.com.cardapio.application.port.out.menu.OptionGroupWithItems;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.exception.OptionGroupNotFoundException;
import br.com.cardapio.domain.menu.model.OptionGroupId;

/** "O grupo existe, é deste restaurante e está ativo?" Desativado conta como inexistente: 404. */
final class OptionLookups {

    private OptionLookups() {
    }

    static OptionGroupWithItems activeGroup(OptionGroupPort groups, EstablishmentId establishmentId,
                                            OptionGroupId id) {
        return groups.find(establishmentId, id)
                .filter(g -> g.group().active())
                .orElseThrow(OptionGroupNotFoundException::new);
    }
}
