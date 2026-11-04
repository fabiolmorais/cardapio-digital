package br.com.cardapio.application.port.in.rodizio;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.rodizio.model.RodizioId;

import java.util.Map;
import java.util.Objects;

/** @param guests pessoas por faixa, pelo nome da faixa */
public record ActivateRodizioCommand(EstablishmentId establishmentId, DiningTableId tableId, RodizioId rodizioId,
                                     Map<String, Integer> guests, UserId by) {

    public ActivateRodizioCommand {
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(tableId);
        Objects.requireNonNull(rodizioId);
        Objects.requireNonNull(by);
        guests = Map.copyOf(guests);
    }
}
