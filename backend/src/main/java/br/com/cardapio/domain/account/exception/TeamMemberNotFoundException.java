package br.com.cardapio.domain.account.exception;

import br.com.cardapio.domain.shared.exception.DomainException;

/**
 * A pessoa não está na equipe deste restaurante (404). Vale também quando ela existe, mas em
 * outro restaurante: a resposta é a mesma, para não revelar o que existe fora daqui.
 */
public class TeamMemberNotFoundException extends DomainException {

    public TeamMemberNotFoundException() {
        super("Pessoa não encontrada na equipe");
    }
}
