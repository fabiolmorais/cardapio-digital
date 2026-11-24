package br.com.cardapio.application.port.in.report;

import br.com.cardapio.domain.report.model.CardTime;
import br.com.cardapio.domain.report.model.OriginPoint;
import br.com.cardapio.domain.report.model.PhoneFunnel;
import br.com.cardapio.domain.report.model.PhoneStats;
import br.com.cardapio.domain.report.model.PresenceCount;
import br.com.cardapio.domain.report.model.ReportPeriod;
import br.com.cardapio.domain.report.model.RodizioItem;
import br.com.cardapio.domain.report.model.RodizioTotals;
import br.com.cardapio.domain.report.model.SeriesGrouping;
import br.com.cardapio.domain.report.model.TierCount;

import java.util.List;

/**
 * Pedido pelo celular e rodízio (desenho 14.7, REL-63 a 75): só no Business.
 *
 * @param grouping como a série de "quem lança os pedidos" foi agrupada
 * @param cardTime o tempo verde e vermelho dos cartões da passagem (desde que as viradas são gravadas)
 */
public record PhoneRodizioReport(ReportPeriod period, SeriesGrouping grouping, List<OriginPoint> origin,
                                 PhoneFunnel funnel, PhoneStats phones, List<PresenceCount> presence,
                                 RodizioTotals rodizio, List<TierCount> tiers, List<RodizioItem> topItems,
                                 CardTime cardTime, List<RodizioItem> topCuts) {
}
