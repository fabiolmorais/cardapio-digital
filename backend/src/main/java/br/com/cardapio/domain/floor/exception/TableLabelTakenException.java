package br.com.cardapio.domain.floor.exception;

import br.com.cardapio.domain.shared.exception.ConflictException;

/** O rótulo já existe no restaurante, inclusive numa mesa desativada (aí é só reativá-la). */
public class TableLabelTakenException extends ConflictException {

    public TableLabelTakenException(String label) {
        super("Já existe uma mesa \"" + label + "\". Se ela estiver desativada, é só ativá-la de novo.");
    }
}
