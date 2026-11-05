package br.com.cardapio.domain.push.exception;

import br.com.cardapio.domain.shared.exception.DomainException;

/** 422: o servidor está sem as chaves VAPID (o push está desligado). */
public class PushDisabledException extends DomainException {

    public PushDisabledException() {
        super("As notificações no celular estão desligadas neste servidor.");
    }
}
