package br.com.cardapio.adapter.out.persistence.menu;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentFilter;
import br.com.cardapio.application.port.out.menu.MenuCategoryOverview;
import br.com.cardapio.application.port.out.menu.MenuCategoryPort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.exception.MenuCategoryNameTakenException;
import br.com.cardapio.domain.menu.model.MenuCategory;
import br.com.cardapio.domain.menu.model.MenuCategoryId;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class MenuCategoryPersistenceAdapter implements MenuCategoryPort {

    /** Um id que nunca existe, para "não excluir ninguém" na consulta de nome repetido. */
    private static final UUID NONE = new UUID(0, 0);

    private final MenuCategoryJpaRepository categories;
    private final ProductJpaRepository products;
    private final EstablishmentFilter establishmentFilter;

    MenuCategoryPersistenceAdapter(MenuCategoryJpaRepository categories, ProductJpaRepository products,
                                   EstablishmentFilter establishmentFilter) {
        this.categories = categories;
        this.products = products;
        this.establishmentFilter = establishmentFilter;
    }

    /** Como no setor: saveAndFlush, e o índice único do nome vira o mesmo 409 da checagem. */
    @Override
    public void save(MenuCategory category) {
        establishmentFilter.enable(category.establishmentId());
        try {
            categories.saveAndFlush(MenuPersistenceMapper.toEntity(category));
        } catch (DataIntegrityViolationException e) {
            throw new MenuCategoryNameTakenException(category.name());
        }
    }

    @Override
    public Optional<MenuCategory> find(EstablishmentId establishmentId, MenuCategoryId id) {
        establishmentFilter.enable(establishmentId);
        return categories.findByIdAndEstablishmentId(id.value(), establishmentId.value())
                .map(MenuPersistenceMapper::toDomain);
    }

    @Override
    public ResultPage<MenuCategoryOverview> findOverview(EstablishmentId establishmentId, PageQuery page) {
        establishmentFilter.enable(establishmentId);
        Page<MenuCategoryRow> rows = categories.findOverview(establishmentId.value(),
                PageRequest.of(page.page(), page.size()));
        return new ResultPage<>(rows.map(MenuPersistenceMapper::toDomain).getContent(),
                page.page(), page.size(), rows.getTotalElements());
    }

    @Override
    public List<MenuCategory> findActive(EstablishmentId establishmentId) {
        establishmentFilter.enable(establishmentId);
        return categories.findByEstablishmentIdAndActiveTrueOrderByPositionAscIdAsc(establishmentId.value())
                .stream().map(MenuPersistenceMapper::toDomain).toList();
    }

    @Override
    public boolean activeNameExists(EstablishmentId establishmentId, String name, MenuCategoryId exceptId) {
        establishmentFilter.enable(establishmentId);
        return categories.activeNameExists(establishmentId.value(), name, exceptId == null ? NONE : exceptId.value());
    }

    @Override
    public int nextPosition(EstablishmentId establishmentId) {
        establishmentFilter.enable(establishmentId);
        return categories.nextPosition(establishmentId.value());
    }

    @Override
    public long countActiveProducts(EstablishmentId establishmentId, MenuCategoryId categoryId) {
        establishmentFilter.enable(establishmentId);
        return products.countActiveByCategory(establishmentId.value(), categoryId.value());
    }
}
