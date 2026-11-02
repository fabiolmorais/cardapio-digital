package br.com.cardapio.domain.shared.event;

import br.com.cardapio.domain.account.model.EstablishmentId;

import java.time.Instant;

/**
 * Algo que aconteceu no negócio e que outras partes (as telas, por enquanto) querem saber.
 * Todo evento é de um restaurante: é por ele que o tempo real decide quem recebe.
 */
public interface DomainEvent {

    EstablishmentId establishmentId();

    /** Quando aconteceu. Serve para medir quanto tempo o aviso leva até a tela. */
    Instant occurredAt();
}
