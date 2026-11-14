package br.com.cardapio.domain.menu.exception;

import br.com.cardapio.domain.shared.exception.NotFoundException;

public class OptionGroupNotFoundException extends NotFoundException {

    public OptionGroupNotFoundException() {
        super("Grupo de opções não encontrado");
    }
}
