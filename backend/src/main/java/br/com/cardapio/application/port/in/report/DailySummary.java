package br.com.cardapio.application.port.in.report;

import br.com.cardapio.domain.report.model.HourlyRevenue;
import br.com.cardapio.domain.report.model.GoalProgress;
import br.com.cardapio.domain.report.model.ItemSales;
import br.com.cardapio.domain.report.model.PaymentTotal;
import br.com.cardapio.domain.report.model.ReportTotals;
import br.com.cardapio.domain.report.model.SlowAttention;
import br.com.cardapio.domain.report.model.SummaryWindow;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * O resumo de um dia (desenho 14.8, REL-76), com os blocos do Pro: faturamento, comandas e tíquete
 * contra o mesmo dia da semana anterior, o preparo médio, o campeão, o pico, o ponto de atenção e
 * as formas de pagamento; no Business, a meta do mês (vazia sem meta).
 *
 * @param champion o produto que mais faturou
 * @param peak     a hora em que chegaram as mesas que mais faturaram
 */
public record DailySummary(String establishmentName, SummaryWindow window, ReportTotals current,
                           ReportTotals compared, Optional<Duration> averagePrep,
                           Optional<Duration> comparedAveragePrep, Optional<ItemSales> champion,
                           Optional<HourlyRevenue> peak, Optional<SlowAttention> attention,
                           List<PaymentTotal> payments, Optional<GoalProgress> goal) {

    /** Dia sem nenhuma comanda fechada não gera resumo. */
    public boolean hasActivity() {
        return current.tabs() > 0;
    }
}
