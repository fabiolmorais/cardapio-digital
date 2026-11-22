package br.com.cardapio.application.port.out.report;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.SectorId;
import br.com.cardapio.domain.report.model.CallTotals;
import br.com.cardapio.domain.report.model.CallTypeCount;
import br.com.cardapio.domain.report.model.DuplicateWarnings;
import br.com.cardapio.domain.report.model.HourDuration;
import br.com.cardapio.domain.report.model.ShiftLoad;
import br.com.cardapio.domain.report.model.TableCalls;
import br.com.cardapio.domain.report.model.WaiterStats;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * O atendimento e a equipe (ADR 0013), no intervalo [início, fim): os chamados pela criação, os itens
 * pela entrega, as comandas pelo fechamento e os pedidos pelo envio. O setor é o da mesa.
 */
public interface TeamReportPort {

    /** Quantos chamados, a resposta e a conclusão médias, e de pedir a conta até a comanda fechar. */
    CallTotals calls(EstablishmentId establishmentId, Instant start, Instant end, Optional<SectorId> sector);

    /** Cada pessoa que fez algo no período (aceitou chamado, entregou, fechou ou lançou pedido). */
    List<WaiterStats> waiters(EstablishmentId establishmentId, Instant start, Instant end, Optional<SectorId> sector);

    /** A resposta média ao chamado pela hora (no fuso) em que ele foi feito. */
    List<HourDuration> responseByHour(EstablishmentId establishmentId, Instant start, Instant end, ZoneId zone,
                                      Optional<SectorId> sector);

    /** Os chamados por tipo, com os cancelados pelo cliente. */
    List<CallTypeCount> byType(EstablishmentId establishmentId, Instant start, Instant end, Optional<SectorId> sector);

    /** Os chamados e as comandas fechadas de cada mesa (o nome e o setor de hoje). */
    List<TableCalls> tables(EstablishmentId establishmentId, Instant start, Instant end, Optional<SectorId> sector);

    /** Os avisos de pedido repetido mostrados e confirmados. */
    DuplicateWarnings duplicateWarnings(EstablishmentId establishmentId, Instant start, Instant end,
                                        Optional<SectorId> sector);

    /** Cada hora (de cada dia, no fuso) com chamados, e quantas pessoas do salão estavam no turno nela. */
    List<ShiftLoad> shiftLoads(EstablishmentId establishmentId, Instant start, Instant end, ZoneId zone,
                               Optional<SectorId> sector);
}
