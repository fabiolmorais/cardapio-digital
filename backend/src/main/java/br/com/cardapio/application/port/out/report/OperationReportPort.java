package br.com.cardapio.application.port.out.report;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.SectorId;
import br.com.cardapio.domain.report.model.CanceledProductCount;
import br.com.cardapio.domain.report.model.OperationTotals;
import br.com.cardapio.domain.report.model.WaiterCalls;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Os números de operação dos relatórios completos (ADR 0013), no intervalo [início, fim). */
public interface OperationReportPort {

    /**
     * Tempo até a mesa (itens pedidos no intervalo e entregues), pedidos pelo celular e pela equipe
     * (enviados no intervalo), resposta ao chamado (chamados do intervalo aceitos), permanência e valor
     * cancelado (comandas fechadas no intervalo). O setor é o da mesa.
     */
    OperationTotals totals(EstablishmentId establishmentId, Instant start, Instant end, Optional<SectorId> sector);

    /** Os garçons que mais aceitaram chamados no intervalo, com a resposta média. */
    List<WaiterCalls> waiters(EstablishmentId establishmentId, Instant start, Instant end, Optional<SectorId> sector,
                              int limit);

    /** As mesas das comandas ainda abertas que abriram antes de {@code openedBefore}, da mais antiga. */
    List<String> forgottenTabs(EstablishmentId establishmentId, Instant openedBefore, Optional<SectorId> sector);

    /** Os itens cancelados no intervalo, por produto (o nome de hoje) e motivo, somando as quantidades. */
    List<CanceledProductCount> canceled(EstablishmentId establishmentId, Instant start, Instant end,
                                        Optional<SectorId> sector);
}
