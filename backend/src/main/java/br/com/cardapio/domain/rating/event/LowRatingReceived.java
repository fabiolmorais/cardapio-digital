package br.com.cardapio.domain.rating.event;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.rating.model.RatingId;
import br.com.cardapio.domain.shared.event.DomainEvent;

import java.time.Instant;
import java.util.Objects;

/**
 * Uma mesa deu nota 1 ou 2 ao atendimento: o painel mostra o alerta ao gerente na hora, e o push
 * avisa o celular de dono e gerente. Só ids e a nota, como os outros eventos; não vai para a mesa.
 */
public record LowRatingReceived(
        EstablishmentId establishmentId,
        DiningTableId tableId,
        RatingId ratingId,
        int stars,
        Instant occurredAt
) implements DomainEvent {

    public LowRatingReceived {
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(tableId);
        Objects.requireNonNull(ratingId);
        Objects.requireNonNull(occurredAt);
    }
}
