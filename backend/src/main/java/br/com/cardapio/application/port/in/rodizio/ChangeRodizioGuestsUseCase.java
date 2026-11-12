package br.com.cardapio.application.port.in.rodizio;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.floor.model.DiningTableId;

import java.util.Map;

/** Chegou ou saiu alguém: o garçom ajusta as pessoas, até a conta. */
public interface ChangeRodizioGuestsUseCase {

    /** @param by quem ajustou (termina o "Quero o rodízio" de quem chegou depois) */
    TableRodizio change(EstablishmentId establishmentId, DiningTableId tableId, Map<String, Integer> guests,
                        UserId by);
}
