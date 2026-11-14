package br.com.cardapio.adapter.out.persistence.menu;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentFilter;
import br.com.cardapio.application.port.out.menu.OptionGroupPort;
import br.com.cardapio.application.port.out.menu.OptionGroupWithItems;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.OptionGroup;
import br.com.cardapio.domain.menu.model.OptionGroupId;
import br.com.cardapio.domain.menu.model.OptionItem;
import br.com.cardapio.domain.menu.model.OptionItemId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A biblioteca de grupos de opções. Ler uma página são três consultas (os grupos, as opções de
 * todos eles e em quantos produtos cada um está), qualquer que seja o tamanho dela: sem N+1.
 */
@Component
class OptionGroupPersistenceAdapter implements OptionGroupPort {

    private final OptionGroupJpaRepository groups;
    private final OptionItemJpaRepository items;
    private final ProductOptionGroupJpaRepository links;
    private final EstablishmentFilter establishmentFilter;

    OptionGroupPersistenceAdapter(OptionGroupJpaRepository groups, OptionItemJpaRepository items,
                                  ProductOptionGroupJpaRepository links, EstablishmentFilter establishmentFilter) {
        this.groups = groups;
        this.items = items;
        this.links = links;
        this.establishmentFilter = establishmentFilter;
    }

    @Override
    public void save(OptionGroup group, List<OptionItem> groupItems) {
        establishmentFilter.enable(group.establishmentId());
        groups.save(OptionMapper.toEntity(group));
        items.saveAll(groupItems.stream().map(OptionMapper::toEntity).toList());
    }

    @Override
    public Optional<OptionGroupWithItems> find(EstablishmentId establishmentId, OptionGroupId id) {
        establishmentFilter.enable(establishmentId);
        return groups.findByIdAndEstablishmentId(id.value(), establishmentId.value())
                .map(e -> withItems(establishmentId, List.of(e)).getFirst());
    }

    @Override
    public ResultPage<OptionGroupWithItems> findActive(EstablishmentId establishmentId, PageQuery page) {
        establishmentFilter.enable(establishmentId);
        Page<OptionGroupJpaEntity> found = groups.findByEstablishmentIdAndActiveTrueOrderByNameAscIdAsc(
                establishmentId.value(), PageRequest.of(page.page(), page.size()));
        return new ResultPage<>(withItems(establishmentId, found.getContent()), page.page(), page.size(),
                found.getTotalElements());
    }

    @Override
    public List<OptionGroup> findActiveByIds(EstablishmentId establishmentId, Collection<OptionGroupId> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        establishmentFilter.enable(establishmentId);
        return groups.findByEstablishmentIdAndActiveTrueAndIdIn(establishmentId.value(),
                ids.stream().map(OptionGroupId::value).toList()).stream().map(OptionMapper::toDomain).toList();
    }

    @Override
    public long countActive(EstablishmentId establishmentId) {
        establishmentFilter.enable(establishmentId);
        return groups.countByEstablishmentIdAndActiveTrue(establishmentId.value());
    }

    @Override
    public Optional<OptionItem> findItem(EstablishmentId establishmentId, OptionItemId id) {
        establishmentFilter.enable(establishmentId);
        return items.findByIdAndEstablishmentId(id.value(), establishmentId.value()).map(OptionMapper::toDomain);
    }

    @Override
    public void saveItem(OptionItem item) {
        establishmentFilter.enable(item.establishmentId());
        items.save(OptionMapper.toEntity(item));
    }

    private List<OptionGroupWithItems> withItems(EstablishmentId establishmentId, List<OptionGroupJpaEntity> found) {
        if (found.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = found.stream().map(OptionGroupJpaEntity::getId).toList();
        Map<UUID, List<OptionItem>> byGroup = items
                .findByEstablishmentIdAndOptionGroupIdInAndActiveTrueOrderByPositionAsc(establishmentId.value(), ids)
                .stream().map(OptionMapper::toDomain)
                .collect(Collectors.groupingBy(i -> i.groupId().value()));
        Map<UUID, Long> products = links.countActiveProducts(establishmentId.value(), ids).stream()
                .collect(Collectors.toMap(row -> (UUID) row[0], row -> (Long) row[1]));
        return found.stream().map(e -> new OptionGroupWithItems(OptionMapper.toDomain(e),
                byGroup.getOrDefault(e.getId(), List.of()), products.getOrDefault(e.getId(), 0L))).toList();
    }
}
