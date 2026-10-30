package br.com.cardapio.domain.menu.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.shared.exception.InvalidValueException;

import java.util.Objects;

/** Uma aba do cardápio (Entradas, Sushis, Bebidas). Os produtos ficam dentro de uma categoria. */
public record MenuCategory(
        MenuCategoryId id,
        EstablishmentId establishmentId,
        String name,
        int position,
        boolean active
) {
    public static final int NAME_MAX = 40;

    public MenuCategory {
        Objects.requireNonNull(id);
        Objects.requireNonNull(establishmentId);
        name = validName(name);
        if (position < 0) {
            throw new IllegalArgumentException("posição não pode ser negativa");
        }
    }

    /** Categoria nova entra no fim da lista. */
    public static MenuCategory create(EstablishmentId establishmentId, String name, int position) {
        return new MenuCategory(MenuCategoryId.newId(), establishmentId, name, position, true);
    }

    public MenuCategory rename(String newName) {
        return new MenuCategory(id, establishmentId, newName, position, active);
    }

    /** Novo lugar na lista, depois de arrastar. */
    public MenuCategory at(int newPosition) {
        return new MenuCategory(id, establishmentId, name, newPosition, active);
    }

    public MenuCategory deactivate() {
        return new MenuCategory(id, establishmentId, name, position, false);
    }

    private static String validName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidValueException("Dê um nome à categoria, como \"Entradas\"");
        }
        String trimmed = name.trim();
        if (trimmed.length() > NAME_MAX) {
            throw new InvalidValueException("Nome da categoria longo demais (máximo " + NAME_MAX + " caracteres)");
        }
        return trimmed;
    }
}
