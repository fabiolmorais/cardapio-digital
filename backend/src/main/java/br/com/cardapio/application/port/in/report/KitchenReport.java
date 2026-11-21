package br.com.cardapio.application.port.in.report;

import br.com.cardapio.domain.report.model.ReportLevel;
import br.com.cardapio.domain.report.model.ReportPeriod;
import br.com.cardapio.domain.report.model.SlowItem;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Cozinha e tempos (desenho 14.4), com os blocos básicos (REL-30).
 *
 * @param averagePrep         o preparo médio do período; vazio sem nenhum item pronto
 * @param comparedAveragePrep o da comparação (até a mesma hora, se o período termina hoje)
 * @param preparations        quantos itens ficaram prontos no período
 */
public record KitchenReport(ReportLevel level, ReportPeriod period, ReportPeriod comparison,
                            Optional<Duration> averagePrep, Optional<Duration> comparedAveragePrep, int preparations,
                            List<SlowItem> slowest) {
}
