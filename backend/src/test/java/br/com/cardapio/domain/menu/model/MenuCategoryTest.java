package br.com.cardapio.domain.menu.model;


import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MenuCategoryTest {

    private final EstablishmentId sakura = EstablishmentId.newId();

    @Test
    void createTrimsTheNameAndStartsActive() {
        MenuCategory entradas = MenuCategory.create(sakura, "  Entradas ", 0);

        assertThat(entradas.name()).isEqualTo("Entradas");
        assertThat(entradas.active()).isTrue();
    }

    @Test
    void renameMoveAndDeactivateKeepTheRest() {
        MenuCategory entradas = MenuCategory.create(sakura, "Entradas", 0);

        MenuCategory changed = entradas.rename("Porções").at(3).deactivate();

        assertThat(changed.id()).isEqualTo(entradas.id());
        assertThat(changed.name()).isEqualTo("Porções");
        assertThat(changed.position()).isEqualTo(3);
        assertThat(changed.active()).isFalse();
    }

    @Test
    void needsAShortName() {
        assertThatThrownBy(() -> MenuCategory.create(sakura, " ", 0)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> MenuCategory.create(sakura, "x".repeat(41), 0))
                .isInstanceOf(InvalidValueException.class);
    }
}