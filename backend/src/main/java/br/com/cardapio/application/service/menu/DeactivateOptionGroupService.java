package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.in.menu.DeactivateOptionGroupUseCase;
import br.com.cardapio.application.port.out.menu.OptionGroupPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.OptionGroupId;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Tira o grupo da biblioteca. Ele some de todos os produtos que o usavam (o cardápio só mostra
 * grupos ativos); os pedidos já feitos continuam com a cópia das escolhas.
 */
public class DeactivateOptionGroupService implements DeactivateOptionGroupUseCase {

    private final OptionGroupPort groups;

    public DeactivateOptionGroupService(OptionGroupPort groups) {
        this.groups = groups;
    }

    @Override
    @Transactional
    public void deactivate(EstablishmentId establishmentId, OptionGroupId id) {
        var current = OptionLookups.activeGroup(groups, establishmentId, id);
        groups.save(current.group().deactivate(), List.of());
    }
}
