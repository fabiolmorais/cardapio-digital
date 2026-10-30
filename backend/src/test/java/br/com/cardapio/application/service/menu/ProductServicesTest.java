package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.in.menu.ProductForm;
import br.com.cardapio.application.port.in.menu.ProductSummary;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.exception.MenuCategoryNotFoundException;
import br.com.cardapio.domain.menu.exception.ProductNotFoundException;
import br.com.cardapio.domain.menu.model.MenuCategory;
import br.com.cardapio.domain.menu.model.MenuCategoryId;
import br.com.cardapio.domain.menu.model.Product;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.shared.exception.OutdatedListException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductServicesTest {

    private final MenuFakes fakes = new MenuFakes();
    private final ListProductsService list = new ListProductsService(fakes);
    private final GetProductService get = new GetProductService(fakes);
    private final CreateProductService create = new CreateProductService(fakes, fakes);
    private final UpdateProductService update = new UpdateProductService(fakes, fakes);
    private final ChangeProductAvailabilityService availability = new ChangeProductAvailabilityService(fakes);
    private final DeactivateProductService deactivate = new DeactivateProductService(fakes);
    private final ReorderProductsService reorder = new ReorderProductsService(fakes, fakes);
    private final EstablishmentId sakura = EstablishmentId.newId();
    private MenuCategory entradas;
    private MenuCategory quentes;

    @BeforeEach
    void menu() {
        entradas = fakes.givenCategory(sakura, "Entradas");
        quentes = fakes.givenCategory(sakura, "Pratos quentes");
    }

    private static ProductForm form(MenuCategoryId category, String name, long price, Long promo) {
        return new ProductForm(category, name, "", price, promo, false, true);
    }

    private List<String> names(MenuCategoryId category, String text) {
        return list.list(sakura, category, text, new PageQuery(0, 50)).content().stream()
                .map(ProductSummary::name).toList();
    }

    @Test
    void newProductsGoToTheEndOfTheirCategory() {
        create.create(sakura, form(entradas.id(), "Guioza", 2990, null));
        ProductSummary sunomono = create.create(sakura, form(entradas.id(), "Sunomono", 1990, 1590L));

        assertThat(sunomono.position()).isEqualTo(1);
        assertThat(sunomono.promoPriceCents()).isEqualTo(1590L);
        assertThat(names(entradas.id(), null)).containsExactly("Guioza", "Sunomono");
    }

    @Test
    void theListFollowsTheMenuOrderAndSearchesByName() {
        fakes.givenProduct(quentes, "Yakisoba", 4990);
        fakes.givenProduct(entradas, "Guioza", 2990);
        fakes.givenProduct(entradas, "Hot roll", 2790);

        assertThat(names(null, null)).containsExactly("Guioza", "Hot roll", "Yakisoba");
        assertThat(names(null, "  ROLL ")).containsExactly("Hot roll");
        assertThat(names(null, " ")).hasSize(3);
    }

    @Test
    void theFormReplacesEverythingAndMovingGoesToTheEnd() {
        fakes.givenProduct(quentes, "Yakisoba", 4990);
        ProductSummary guioza = create.create(sakura, form(entradas.id(), "Guioza", 2990, 2490L));

        ProductSummary moved = update.update(sakura, guioza.id(),
                new ProductForm(quentes.id(), "Guioza grelhado", "6 unidades", 3290, null, true, false));

        assertThat(moved.categoryId()).isEqualTo(quentes.id());
        assertThat(moved.position()).isEqualTo(1);
        assertThat(moved.promoPriceCents()).as("promoção vazia apaga a promoção").isNull();
        assertThat(moved.featured()).isTrue();
        assertThat(moved.available()).isFalse();
        assertThat(get.get(sakura, guioza.id()).name()).isEqualTo("Guioza grelhado");
    }

    @Test
    void invalidPricesAreRejected() {
        assertThatThrownBy(() -> create.create(sakura, form(entradas.id(), "Guioza", 2990, 2990L)))
                .isInstanceOf(InvalidValueException.class);
        assertThat(fakes.products).isEmpty();
    }

    @Test
    void soldOutStaysInTheMenuButDeactivatedLeaves() {
        Product guioza = fakes.givenProduct(entradas, "Guioza", 2990);

        assertThat(availability.change(sakura, guioza.id(), false).available()).isFalse();
        assertThat(names(entradas.id(), null)).containsExactly("Guioza");

        deactivate.deactivate(sakura, guioza.id());
        deactivate.deactivate(sakura, guioza.id()); // de novo: tudo bem
        assertThat(names(entradas.id(), null)).isEmpty();
        assertThatThrownBy(() -> get.get(sakura, guioza.id())).isInstanceOf(ProductNotFoundException.class);
        assertThatThrownBy(() -> availability.change(sakura, guioza.id(), true))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void reorderingWorksInsideOneCategory() {
        Product guioza = fakes.givenProduct(entradas, "Guioza", 2990);
        Product sunomono = fakes.givenProduct(entradas, "Sunomono", 1990);
        Product edamame = fakes.givenProduct(entradas, "Edamame", 1990);
        int savesBefore = fakes.productSaves;

        reorder.reorder(sakura, entradas.id(), List.of(edamame.id(), guioza.id(), sunomono.id()));

        assertThat(names(entradas.id(), null)).containsExactly("Edamame", "Guioza", "Sunomono");
        assertThat(fakes.productSaves - savesBefore).isEqualTo(3);
        assertThatThrownBy(() -> reorder.reorder(sakura, entradas.id(), List.of(guioza.id(), sunomono.id())))
                .isInstanceOf(OutdatedListException.class);
    }

    @Test
    void anotherRestaurantsCategoryOrProductIsNotFound() {
        MenuCategory elsewhere = fakes.givenCategory(EstablishmentId.newId(), "Entradas");
        Product theirs = fakes.givenProduct(elsewhere, "Guioza", 2990);

        assertThatThrownBy(() -> create.create(sakura, form(elsewhere.id(), "Guioza", 2990, null)))
                .isInstanceOf(MenuCategoryNotFoundException.class);
        assertThatThrownBy(() -> get.get(sakura, theirs.id())).isInstanceOf(ProductNotFoundException.class);
        assertThatThrownBy(() -> update.update(sakura, theirs.id(), form(entradas.id(), "Meu", 100, null)))
                .isInstanceOf(ProductNotFoundException.class);
        assertThatThrownBy(() -> deactivate.deactivate(sakura, theirs.id()))
                .isInstanceOf(ProductNotFoundException.class);
        assertThatThrownBy(() -> reorder.reorder(sakura, elsewhere.id(), List.of(theirs.id())))
                .isInstanceOf(MenuCategoryNotFoundException.class);

        ProductSummary mine = create.create(sakura, form(entradas.id(), "Edamame", 1990, null));
        assertThatThrownBy(() -> update.update(sakura, mine.id(), form(elsewhere.id(), "Edamame", 1990, null)))
                .isInstanceOf(MenuCategoryNotFoundException.class);
    }
}
