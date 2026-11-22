package br.com.cardapio.application.port.out.report;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.SectorId;
import br.com.cardapio.domain.report.model.AdvancePayments;
import br.com.cardapio.domain.report.model.CancelReasonTotal;
import br.com.cardapio.domain.report.model.DailyRevenue;
import br.com.cardapio.domain.report.model.HourlyRevenue;
import br.com.cardapio.domain.report.model.ItemSales;
import br.com.cardapio.domain.report.model.PaymentTotal;
import br.com.cardapio.domain.report.model.PerPersonTicket;
import br.com.cardapio.domain.report.model.ReportPeriod;
import br.com.cardapio.domain.report.model.ReportTotals;
import br.com.cardapio.domain.report.model.SectorSales;
import br.com.cardapio.domain.report.model.TableSales;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * As somas das vendas (ADR 0013): sempre das comandas fechadas ({@code CLOSED}) do restaurante, pelo
 * {@code closed_at}, no intervalo [início, fim). O setor, quando vem, é o da mesa da comanda.
 */
public interface SalesReportPort {

    /** Faturamento e número de comandas. */
    ReportTotals totals(EstablishmentId establishmentId, Instant start, Instant end, Optional<SectorId> sector);

    /** Por dia do restaurante (no fuso dado), no intervalo [início, fim); só os dias com comandas. */
    List<DailyRevenue> revenueByDay(EstablishmentId establishmentId, Instant start, Instant end, ZoneId zone,
                                    Optional<SectorId> sector);

    /** O que entrou por forma de pagamento, com a taxa de serviço (os pagamentos das comandas fechadas no período). */
    List<PaymentTotal> payments(EstablishmentId establishmentId, Instant start, Instant end, Optional<SectorId> sector);

    /** Por dia da semana e hora em que a comanda abriu (no fuso do período); só as horas com comandas. */
    default List<HourlyRevenue> revenueByWeekdayAndHour(EstablishmentId establishmentId, ReportPeriod period,
                                                        Optional<SectorId> sector) {
        return revenueByWeekdayAndHour(establishmentId, period.start(), period.end(), period.zone(), sector);
    }

    /** O mesmo, num intervalo [início, fim) qualquer (o "dia" do resumo diário, que não começa à meia-noite). */
    List<HourlyRevenue> revenueByWeekdayAndHour(EstablishmentId establishmentId, Instant start, Instant end,
                                                ZoneId zone, Optional<SectorId> sector);

    /** Os produtos que mais faturaram (sem os itens cancelados e sem as rodadas do rodízio). */
    List<ItemSales> topItems(EstablishmentId establishmentId, Instant start, Instant end, Optional<SectorId> sector,
                             int limit);

    /** Por setor (o nome de hoje), com as mesas ativas de cada um; setores sem venda entram com zero. */
    List<SectorSales> bySector(EstablishmentId establishmentId, Instant start, Instant end, Optional<SectorId> sector);

    /** As mesas que mais faturaram (o nome e o setor de hoje). */
    List<TableSales> topTables(EstablishmentId establishmentId, Instant start, Instant end, Optional<SectorId> sector,
                               int limit);

    /** As comandas com rodízio: o faturamento e as pessoas do rodízio. */
    PerPersonTicket rodizioTicket(EstablishmentId establishmentId, Instant start, Instant end,
                                  Optional<SectorId> sector);

    /** As comandas sem rodízio em que o garçom informou as pessoas: o faturamento e as pessoas. */
    PerPersonTicket peopleTicket(EstablishmentId establishmentId, Instant start, Instant end,
                                 Optional<SectorId> sector);

    /** Os pagamentos recebidos antes do fechamento da comanda: quantas comandas e quanto. */
    AdvancePayments advancePayments(EstablishmentId establishmentId, Instant start, Instant end,
                                    Optional<SectorId> sector);

    /** Os itens cancelados nas comandas fechadas, somados por motivo (null: sem motivo). */
    List<CancelReasonTotal> canceledByReason(EstablishmentId establishmentId, Instant start, Instant end,
                                             Optional<SectorId> sector);
}
