package br.com.cardapio.domain.account.exception;

import br.com.cardapio.domain.shared.exception.DomainException;

/**
 * 422: o link não existe, venceu ou já foi usado. Uma frase só para os três casos: quem tenta
 * adivinhar um link não descobre nada.
 */
public class PasswordResetLinkInvalidException extends DomainException {

    public PasswordResetLinkInvalidException() {
        super("Este link para redefinir a senha expirou ou já foi usado. Peça outro.");
    }
}
