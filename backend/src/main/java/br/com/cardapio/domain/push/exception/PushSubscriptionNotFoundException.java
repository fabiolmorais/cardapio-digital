package br.com.cardapio.domain.push.exception;

import br.com.cardapio.domain.shared.exception.NotFoundException;

public class PushSubscriptionNotFoundException extends NotFoundException {

    public PushSubscriptionNotFoundException() {
        super("Este navegador não está inscrito nas notificações.");
    }
}
