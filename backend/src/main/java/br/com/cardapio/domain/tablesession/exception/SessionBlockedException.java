package br.com.cardapio.domain.tablesession.exception;

import br.com.cardapio.domain.shared.exception.DomainException;

/** 403: a equipe bloqueou este celular nesta visita. */
public class SessionBlockedException extends DomainException {

    public SessionBlockedException() {
        super("A equipe do restaurante bloqueou os chamados deste celular. Fale com um garçom.");
    }
}
