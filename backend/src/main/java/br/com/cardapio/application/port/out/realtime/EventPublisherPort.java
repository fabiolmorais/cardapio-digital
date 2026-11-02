package br.com.cardapio.application.port.out.realtime;

import br.com.cardapio.domain.shared.event.DomainEvent;

/**
 * Avisa que algo aconteceu. Dentro de uma transação, o aviso só sai se ela for confirmada
 * (commit): um chamado desfeito por erro nunca aparece na tela de ninguém.
 */
public interface EventPublisherPort {

    void publish(DomainEvent event);
}
