package br.com.cardapio.domain.floor.exception;

import br.com.cardapio.domain.shared.exception.NotFoundException;

public class SectorNotFoundException extends NotFoundException {

    public SectorNotFoundException() {
        super("Setor não encontrado");
    }
}
