package br.com.cardapio.domain.shared.event;

import br.com.cardapio.domain.floor.model.DiningTableId;

/** Um evento de uma mesa: além da equipe, os celulares daquela mesa também recebem. */
public interface TableEvent extends DomainEvent {

    DiningTableId tableId();
}
