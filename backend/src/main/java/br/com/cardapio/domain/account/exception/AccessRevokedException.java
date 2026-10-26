package br.com.cardapio.domain.account.exception;

import br.com.cardapio.domain.shared.exception.DomainException;

/**
 * O token é válido, mas a pessoa não tem mais acesso a este estabelecimento
 * (foi desligada, ou o token aponta para um restaurante que não é dela).
 */
public class AccessRevokedException extends DomainException {

    public AccessRevokedException() {
        super("Seu acesso a este estabelecimento não está ativo. Entre de novo.");
    }
}
