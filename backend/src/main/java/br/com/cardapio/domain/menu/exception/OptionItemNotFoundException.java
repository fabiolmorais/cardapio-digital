package br.com.cardapio.domain.menu.exception;

import br.com.cardapio.domain.shared.exception.NotFoundException;

public class OptionItemNotFoundException extends NotFoundException {

    public OptionItemNotFoundException() {
        super("Opção não encontrada");
    }
}
