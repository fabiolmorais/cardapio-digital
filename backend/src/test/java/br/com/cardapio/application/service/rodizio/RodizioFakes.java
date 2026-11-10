package br.com.cardapio.application.service.rodizio;

import br.com.cardapio.application.port.out.menu.ProductPort;
import br.com.cardapio.application.port.out.rodizio.RodizioPort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.MenuCategoryId;
import br.com.cardapio.domain.menu.model.Product;
import br.com.cardapio.domain.menu.model.ProductDetails;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.rodizio.model.Rodizio;
import br.com.cardapio.domain.rodizio.model.RodizioId;
import br.com.cardapio.domain.shared.model.Money;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Os rodízios e os produtos na memória. Do cardápio (Products), só o que o rodízio usa; o resto
 * lança UnsupportedOperationException.
 */
final class RodizioFakes implements RodizioPort {

    final Map<RodizioId, Rodizio> rodizios = new LinkedHashMap<>();
    final Products products = new Products();

    // --- RodizioPort
    @Override
    public void save(Rodizio rodizio) { rodizios.put(rodizio.id(), rodizio); }

    @Override
    public Optional<Rodizio> find(EstablishmentId establishmentId, RodizioId id) {
        return Optional.ofNullable(rodizios.get(id)).filter(r -> r.establishmentId().equals(establishmentId));
    }

    @Override
    public ResultPage<Rodizio> findAll(EstablishmentId establishmentId, PageQuery page) {
        List<Rodizio> all = rodizios.values().stream()
                .filter(r -> r.establishmentId().equals(establishmentId))
                .sorted(Comparator.comparing((Rodizio r) -> !r.active()).thenComparing(Rodizio::name))
                .toList();
        return new ResultPage<>(all, page.page(), page.size(), all.size());
    }

    @Override
    public ResultPage<Rodizio> findActive(EstablishmentId establishmentId, PageQuery page) {
        List<Rodizio> all = rodizios.values().stream()
                .filter(r -> r.establishmentId().equals(establishmentId) && r.active())
                .sorted(Comparator.comparing(Rodizio::name))
                .toList();
        return new ResultPage<>(all, page.page(), page.size(), all.size());
    }

    @Override
    public long countActive(EstablishmentId establishmentId) {
        return rodizios.values().stream().filter(r -> r.establishmentId().equals(establishmentId) && r.active()).count();
    }

    @Override
    public boolean activeNameExists(EstablishmentId establishmentId, String name, RodizioId exceptId) {
        return rodizios.values().stream().anyMatch(r -> r.establishmentId().equals(establishmentId) && r.active()
                && r.name().equalsIgnoreCase(name) && !r.id().equals(exceptId));
    }

    /** Os produtos (as duas portas têm um countActive igual, por isso a classe à parte). */
    static final class Products implements ProductPort {

        final Map<ProductId, Product> products = new LinkedHashMap<>();

        Product given(EstablishmentId establishmentId, String name) {
            Product product = Product.create(establishmentId, MenuCategoryId.newId(),
                    new ProductDetails(name, "", Money.ofCents(1990), null, false), true, products.size());
            products.put(product.id(), product);
            return product;
        }

        @Override
        public List<Product> findActiveByIds(EstablishmentId establishmentId, Collection<ProductId> ids) {
            return products.values().stream()
                    .filter(p -> p.establishmentId().equals(establishmentId) && p.active() && ids.contains(p.id()))
                    .toList();
        }

        @Override
        public void save(Product product) { throw new UnsupportedOperationException(); }

        @Override
        public Optional<Product> find(EstablishmentId establishmentId, ProductId id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ResultPage<Product> search(EstablishmentId establishmentId, MenuCategoryId categoryId, String text,
                                          PageQuery page) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Product> findActiveByCategory(EstablishmentId establishmentId, MenuCategoryId categoryId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int nextPosition(EstablishmentId establishmentId, MenuCategoryId categoryId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long countActive(EstablishmentId establishmentId) { throw new UnsupportedOperationException(); }

        @Override
        public List<Product> findAllActive(EstablishmentId establishmentId) { throw new UnsupportedOperationException(); }
    }
}
