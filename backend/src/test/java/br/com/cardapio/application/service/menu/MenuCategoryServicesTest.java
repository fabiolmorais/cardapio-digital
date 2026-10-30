package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.in.menu.MenuCategorySummary;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.exception.MenuCategoryNameTakenException;
import br.com.cardapio.domain.menu.exception.MenuCategoryNotEmptyException;
import br.com.cardapio.domain.menu.exception.MenuCategoryNotFoundException;
import br.com.cardapio.domain.menu.model.MenuCategory;
import br.com.cardapio.domain.menu.model.MenuCategoryId;
import br.com.cardapio.domain.shared.exception.OutdatedListException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

class MenuCategoryServicesTest {

    private final MenuFakes fakes = new MenuFakes();
    private final ListMenuCategoriesService list = new ListMenuCategoriesService(fakes);
    private final CreateMenuCategoryService create = new CreateMenuCategoryService(fakes);
    private final RenameMenuCategoryService rename = new RenameMenuCategoryService(fakes);
    private final DeactivateMenuCategoryService deactivate = new DeactivateMenuCategoryService(fakes);
    private final ReorderMenuCategoriesService reorder = new ReorderMenuCategoriesService(fakes);
    private final EstablishmentId sakura = EstablishmentId.newId();

    private List<String> names() {
        return list.list(sakura, new PageQuery(0, 50)).content().stream().map(MenuCategorySummary::name).toList();
    }

    @Test
    void newCategoriesGoToTheEndWithTheirProductCount() {
        MenuCategory entradas = fakes.givenCategory(sakura, "Entradas");
        fakes.givenProduct(entradas, "Guioza", 2990);
        create.create(sakura, "Sushis");

        assertThat(list.list(sakura, new PageQuery(0, 50)).content())
                .extracting(MenuCategorySummary::name, MenuCategorySummary::activeProducts)
                .containsExactly(tuple("Entradas", 1L), tuple("Sushis", 0L));
    }

    @Test
    void namesDoNotRepeatIgnoringCase() {
        create.create(sakura, "Bebidas");
        MenuCategorySummary sushis = create.create(sakura, "Sushis");

        assertThatThrownBy(() -> create.create(sakura, " bebidas ")).isInstanceOf(MenuCategoryNameTakenException.class);
        assertThatThrownBy(() -> rename.rename(sakura, sushis.id(), "BEBIDAS"))
                .isInstanceOf(MenuCategoryNameTakenException.class);
        assertThat(rename.rename(sakura, sushis.id(), "SUSHIS").name()).isEqualTo("SUSHIS");
    }

    @Test
    void categoryWithProductsCannotBeDeactivated() {
        MenuCategory entradas = fakes.givenCategory(sakura, "Entradas");
        var guioza = fakes.givenProduct(entradas, "Guioza", 2990);

        assertThatThrownBy(() -> deactivate.deactivate(sakura, entradas.id()))
                .isInstanceOf(MenuCategoryNotEmptyException.class)
                .hasMessage("Esta categoria tem 1 produto. Mova ou desative o produto antes.");

        fakes.save(guioza.deactivate());
        deactivate.deactivate(sakura, entradas.id());
        deactivate.deactivate(sakura, entradas.id()); // de novo: tudo bem
        assertThat(names()).isEmpty();
    }

    @Test
    void reorderingSavesOnlyWhatMoved() {
        MenuCategory entradas = fakes.givenCategory(sakura, "Entradas");
        MenuCategory sushis = fakes.givenCategory(sakura, "Sushis");
        MenuCategory bebidas = fakes.givenCategory(sakura, "Bebidas");

        reorder.reorder(sakura, List.of(sushis.id(), entradas.id(), bebidas.id()));

        assertThat(names()).containsExactly("Sushis", "Entradas", "Bebidas");
        assertThat(fakes.categories.get(bebidas.id())).isEqualTo(bebidas); // não mudou de lugar
    }

    @Test
    void reorderingWithAnOutdatedListChangesNothing() {
        MenuCategory entradas = fakes.givenCategory(sakura, "Entradas");
        MenuCategory sushis = fakes.givenCategory(sakura, "Sushis");
        fakes.givenCategory(sakura, "Bebidas"); // criada noutra aba, depois que a tela carregou

        assertThatThrownBy(() -> reorder.reorder(sakura, List.of(sushis.id(), entradas.id())))
                .isInstanceOf(OutdatedListException.class);
        assertThat(names()).containsExactly("Entradas", "Sushis", "Bebidas");
    }

    @Test
    void categoryOfAnotherRestaurantIsNotFoundAndCannotBeReordered() {
        MenuCategory elsewhere = fakes.givenCategory(EstablishmentId.newId(), "Entradas");
        MenuCategory mine = fakes.givenCategory(sakura, "Entradas");

        assertThatThrownBy(() -> rename.rename(sakura, elsewhere.id(), "Minhas"))
                .isInstanceOf(MenuCategoryNotFoundException.class);
        assertThatThrownBy(() -> deactivate.deactivate(sakura, elsewhere.id()))
                .isInstanceOf(MenuCategoryNotFoundException.class);
        List<MenuCategoryId> withForeignId = List.of(elsewhere.id(), mine.id());
        assertThatThrownBy(() -> reorder.reorder(sakura, withForeignId)).isInstanceOf(OutdatedListException.class);
    }
}
