package br.com.cardapio.application.port.out.rodizio;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.rodizio.model.TabRodizioId;
import br.com.cardapio.domain.rodizio.model.TabRodizioStatus;

import java.time.Instant;

/** As viradas do cartão da passagem (REL-74). */
public interface RodizioCardHistoryPort {

    void record(EstablishmentId establishmentId, TabRodizioId tabRodizioId, TabRodizioStatus status, Instant at);
}
