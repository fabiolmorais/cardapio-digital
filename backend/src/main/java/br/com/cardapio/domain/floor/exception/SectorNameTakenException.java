package br.com.cardapio.domain.floor.exception;

import br.com.cardapio.domain.shared.exception.ConflictException;

public class SectorNameTakenException extends ConflictException {

    public SectorNameTakenException(String name) {
        super("Já existe um setor chamado \"" + name + "\"");
    }
}
