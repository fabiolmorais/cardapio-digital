package br.com.cardapio.application.port.in.rodizio;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.DiningTableId;

import java.util.Map;

/** Chegou ou saiu alguém: o garçom ajusta as pessoas, até a conta. */
public interface ChangeRodizioGuestsUseCase {

    TableRodizio change(EstablishmentId establishmentId, DiningTableId tableId, Map<String, Integer> guests);
}
