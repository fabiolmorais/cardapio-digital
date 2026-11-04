package br.com.cardapio.application.port.out.rodizio;

import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.rodizio.model.TabRodizioStatus;

import java.time.Instant;

/** Uma mesa no rodízio de passagem, como a tela dos passadores mostra. */
public record PassagemTableView(DiningTableId tableId, String tableLabel, String sectorName, String rodizioName,
                                int people, TabRodizioStatus status, Instant statusChangedAt) {
}
