package br.com.cardapio.domain.account.exception;

import br.com.cardapio.domain.shared.exception.DomainException;

/** Quem pediu a mudança na equipe não tem permissão para ela (403). */
public class TeamPermissionException extends DomainException {

    public TeamPermissionException(String message) {
        super(message);
    }
}
