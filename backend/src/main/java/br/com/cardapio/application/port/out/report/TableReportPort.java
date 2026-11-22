package br.com.cardapio.application.port.out.report;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.SectorId;
import br.com.cardapio.domain.report.model.ForgottenTabs;
import br.com.cardapio.domain.report.model.OccupiedInterval;
import br.com.cardapio.domain.report.model.StayTotals;
import br.com.cardapio.domain.report.model.TableMoves;
import br.com.cardapio.domain.report.model.TableUsage;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * As mesas e a ocupação (ADR 0013), no intervalo [início, fim). O setor é o da mesa onde a comanda
 * está (a de destino, se ela mudou de mesa).
 */
public interface TableReportPort {

    /** As comandas fechadas no período (pelo fechamento), com o tempo de mesa e a permanência. */
    StayTotals stays(EstablishmentId establishmentId, Instant start, Instant end, Optional<SectorId> sector);

    /** Cada mesa ativa, do setor pela posição e da mesa pelo nome, com as comandas fechadas no período. */
    List<TableUsage> usage(EstablishmentId establishmentId, Instant start, Instant end, Optional<SectorId> sector);

    /**
     * As comandas que ocuparam mesa no período (abertas antes do fim, fechadas depois do início ou
     * ainda abertas), da abertura ao fechamento (ou a {@code now}, se abertas). As juntadas a outra não
     * entram.
     */
    List<OccupiedInterval> occupied(EstablishmentId establishmentId, Instant start, Instant end, Instant now,
                                    Optional<SectorId> sector);

    /**
     * As comandas abertas no período que ficaram abertas mais que {@code limit} (fechadas depois disso
     * ou ainda abertas em {@code now}): quantas e as {@code shown} mais recentes.
     */
    ForgottenTabs forgotten(EstablishmentId establishmentId, Instant start, Instant end, Instant now,
                            Duration limit, Optional<SectorId> sector, int shown);

    /** As mudanças e junções de comandas do período, pela mesa de onde saíram. */
    TableMoves moves(EstablishmentId establishmentId, Instant start, Instant end, Optional<SectorId> sector);
}
