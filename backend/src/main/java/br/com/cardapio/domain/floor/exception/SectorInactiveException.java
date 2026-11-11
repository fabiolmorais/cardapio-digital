package br.com.cardapio.domain.floor.exception;

import br.com.cardapio.domain.shared.exception.DomainException;

/** Mesa ativa precisa de setor ativo (422): reative o setor ou mova a mesa para outro. */
public class SectorInactiveException extends DomainException {

    public SectorInactiveException(String sectorName) {
        super("O setor " + sectorName + " está desativado. Reative o setor ou mova a mesa para outro setor.");
    }
}
