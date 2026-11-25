package br.com.cardapio.domain.account.exception;

import br.com.cardapio.domain.shared.exception.DomainException;

/** Só o dono cria unidades (403). */
public class UnitPermissionException extends DomainException {

    public UnitPermissionException() {
        super("Só o dono pode criar unidades");
    }
}
