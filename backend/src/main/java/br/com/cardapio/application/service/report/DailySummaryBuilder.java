package br.com.cardapio.application.service.report;

import br.com.cardapio.application.port.in.report.DailySummary;
import br.com.cardapio.application.port.out.report.KitchenReportPort;
import br.com.cardapio.application.port.out.report.SalesReportPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.report.model.PrepTime;
import br.com.cardapio.domain.report.model.SummaryWindow;
import br.com.cardapio.domain.report.service.DailySummaryAnalysis;
import br.com.cardapio.domain.report.service.KitchenAnalysis;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Monta o resumo de um dia com as mesmas consultas dos relatórios (o resumo nunca discorda da tela). */
final class DailySummaryBuilder {

    private final SalesReportPort sales;
    private final KitchenReportPort kitchen;

    DailySummaryBuilder(SalesReportPort sales, KitchenReportPort kitchen) {
        this.sales = sales;
        this.kitchen = kitchen;
    }

    DailySummary build(EstablishmentId id, String name, SummaryWindow window) {
        SummaryWindow before = window.weekBefore();
        Instant start = window.start();
        Instant end = window.end();
        List<PrepTime> prep = kitchen.prepTimes(id, start, end, window.zone(), Optional.empty(), Optional.empty());
        List<PrepTime> usual = kitchen.prepTimes(id, start.minus(Duration.ofDays(DailySummaryAnalysis.USUAL_DAYS)),
                start, window.zone(), Optional.empty(), Optional.empty());
        List<PrepTime> comparedPrep = kitchen.prepTimes(id, before.start(), before.end(), window.zone(),
                Optional.empty(), Optional.empty());
        return new DailySummary(name, window,
                sales.totals(id, start, end, Optional.empty()),
                sales.totals(id, before.start(), before.end(), Optional.empty()),
                KitchenAnalysis.average(prep), KitchenAnalysis.average(comparedPrep),
                sales.topItems(id, start, end, Optional.empty(), 1).stream().findFirst(),
                DailySummaryAnalysis.peak(sales.revenueByWeekdayAndHour(id, start, end, window.zone(),
                        Optional.empty())),
                DailySummaryAnalysis.attention(prep, usual),
                sales.payments(id, start, end, Optional.empty()));
    }
}
