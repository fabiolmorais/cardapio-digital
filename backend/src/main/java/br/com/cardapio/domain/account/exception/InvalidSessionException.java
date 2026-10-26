package br.com.cardapio.domain.account.exception;

import br.com.cardapio.domain.shared.exception.DomainException;

/**
 * Não deu para renovar a sessão: refresh token ausente, desconhecido, vencido, já usado,
 * ou a pessoa perdeu o acesso. Sempre a mesma mensagem, sem dizer qual foi o caso.
 */
public class InvalidSessionException extends DomainException {

    public InvalidSessionException() {
        super("Sua sessão terminou. Entre de novo.");
    }
}
