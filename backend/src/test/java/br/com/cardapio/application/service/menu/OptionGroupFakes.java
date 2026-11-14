package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.out.menu.OptionGroupPort;
import br.com.cardapio.application.port.out.menu.OptionGroupWithItems;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.OptionGroup;
import br.com.cardapio.domain.menu.model.OptionGroupId;
import br.com.cardapio.domain.menu.model.OptionItem;
import br.com.cardapio.domain.menu.model.OptionItemId;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** A biblioteca de grupos de opções em memória (para os testes da mídia das opções). */
final class OptionGroupFakes implements OptionGroupPort {

    final Map<OptionGroupId, OptionGroup> groups = new LinkedHashMap<>();
    final Map<OptionItemId, OptionItem> items = new LinkedHashMap<>();

    @Override
    public void save(OptionGroup group, List<OptionItem> groupItems) {
        groups.put(group.id(), group);
        groupItems.forEach(this::saveItem);
    }

    @Override
    public Optional<OptionGroupWithItems> find(EstablishmentId establishmentId, OptionGroupId id) {
        return Optional.ofNullable(groups.get(id)).filter(g -> g.establishmentId().equals(establishmentId))
                .map(g -> new OptionGroupWithItems(g, items.values().stream()
                        .filter(i -> i.groupId().equals(id) && i.active()).toList(), 0));
    }

    @Override
    public ResultPage<OptionGroupWithItems> findActive(EstablishmentId establishmentId, PageQuery page) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<OptionGroup> findActiveByIds(EstablishmentId establishmentId, Collection<OptionGroupId> ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long countActive(EstablishmentId establishmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<OptionItem> findItem(EstablishmentId establishmentId, OptionItemId id) {
        return Optional.ofNullable(items.get(id)).filter(i -> i.establishmentId().equals(establishmentId));
    }

    @Override
    public void saveItem(OptionItem item) {
        items.put(item.id(), item);
    }
}
