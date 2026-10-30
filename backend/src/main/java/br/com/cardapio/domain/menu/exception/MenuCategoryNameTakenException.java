package br.com.cardapio.domain.menu.exception;

import br.com.cardapio.domain.shared.exception.ConflictException;

public class MenuCategoryNameTakenException extends ConflictException {

    public MenuCategoryNameTakenException(String name) {
        super("Já existe uma categoria chamada \"" + name + "\"");
    }
}
