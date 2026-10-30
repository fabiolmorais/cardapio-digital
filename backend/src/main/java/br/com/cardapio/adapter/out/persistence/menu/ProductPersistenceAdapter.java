package br.com.cardapio.adapter.out.persistence.menu;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentFilter;
import br.com.cardapio.application.port.out.menu.ProductPort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.MenuCategoryId;
import br.com.cardapio.domain.menu.model.Product;
import br.com.cardapio.domain.menu.model.ProductId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
class ProductPersistenceAdapter implements ProductPort {

    private final ProductJpaRepository products;
    private final EstablishmentFilter establishmentFilter;

    ProductPersistenceAdapter(ProductJpaRepository products, EstablishmentFilter establishmentFilter) {
        this.products = products;
        this.establishmentFilter = establishmentFilter;
    }

    @Override
    public void save(Product product) {
        establishmentFilter.enable(product.establishmentId());
        products.save(MenuPersistenceMapper.toEntity(product));
    }

    @Override
    public Optional<Product> find(EstablishmentId establishmentId, ProductId id) {
        establishmentFilter.enable(establishmentId);
        return products.findByIdAndEstablishmentId(id.value(), establishmentId.value())
                .map(MenuPersistenceMapper::toDomain);
    }

    @Override
    public ResultPage<Product> search(EstablishmentId establishmentId, MenuCategoryId categoryId, String text,
                                      PageQuery page) {
        establishmentFilter.enable(establishmentId);
        PageRequest pageable = PageRequest.of(page.page(), page.size());
        String pattern = likePattern(text);
        Page<ProductJpaEntity> rows = categoryId == null
                ? products.search(establishmentId.value(), pattern, pageable)
                : products.searchInCategory(establishmentId.value(), categoryId.value(), pattern, pageable);
        return new ResultPage<>(rows.map(MenuPersistenceMapper::toDomain).getContent(),
                page.page(), page.size(), rows.getTotalElements());
    }

    @Override
    public List<Product> findActiveByCategory(EstablishmentId establishmentId, MenuCategoryId categoryId) {
        establishmentFilter.enable(establishmentId);
        return products.findByEstablishmentIdAndCategoryIdAndActiveTrueOrderByPositionAscIdAsc(
                        establishmentId.value(), categoryId.value())
                .stream().map(MenuPersistenceMapper::toDomain).toList();
    }

    @Override
    public int nextPosition(EstablishmentId establishmentId, MenuCategoryId categoryId) {
        establishmentFilter.enable(establishmentId);
        return products.nextPosition(establishmentId.value(), categoryId.value());
    }

    /**
     * "50% off" vira "%50!% off%": o texto vai entre curingas e os curingas que o próprio texto
     * tem (% e _) e o caractere de escape (!) ganham um "!" na frente, para valerem como letras.
     * Sem texto, "%" pega tudo.
     */
    static String likePattern(String text) {
        if (text == null) {
            return "%";
        }
        String escaped = text.toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escaped + "%";
    }
}
