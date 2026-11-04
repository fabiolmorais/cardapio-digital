package br.com.cardapio.application.port.in.rodizio;

import br.com.cardapio.application.port.out.rodizio.PassagemTableView;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.rodizio.model.TabRodizioStatus;

import java.time.Instant;

/** Uma mesa na tela dos passadores: onde fica, quantas pessoas e a cor do cartão. */
public record PassagemTable(DiningTableId tableId, String tableLabel, String sectorName, String rodizioName,
                            int people, TabRodizioStatus status, Instant statusChangedAt) {

    public static PassagemTable from(PassagemTableView v) {
        return new PassagemTable(v.tableId(), v.tableLabel(), v.sectorName(), v.rodizioName(), v.people(), v.status(),
                v.statusChangedAt());
    }
}
