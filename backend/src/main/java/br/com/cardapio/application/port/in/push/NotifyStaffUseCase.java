package br.com.cardapio.application.port.in.push;

import br.com.cardapio.domain.shared.event.DomainEvent;

/** Avisa a equipe no celular, quando o evento pede (um chamado novo, um pedido para confirmar). */
public interface NotifyStaffUseCase {

    void notify(DomainEvent event);
}
