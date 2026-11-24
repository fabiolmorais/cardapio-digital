package br.com.cardapio.application.service.report;

import br.com.cardapio.application.port.in.report.DailySummary;
import java.time.YearMonth;
import java.time.LocalDate;
import br.com.cardapio.domain.shared.model.Money;
import br.com.cardapio.domain.report.service.GoalAnalysis;
import br.com.cardapio.domain.report.model.GoalProgress;
import br.com.cardapio.application.port.out.report.RevenueGoalPort;
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
    private final RevenueGoalPort goals;

    DailySummaryBuilder(SalesReportPort sales, KitchenReportPort kitchen, RevenueGoalPort goals) {
        this.sales = sales;
        this.kitchen = kitchen;
        this.goals = goals;
    }

    /** @param withGoal o plano tem a meta (Business) */
    DailySummary build(EstablishmentId id, String name, SummaryWindow window, boolean withGoal) {
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
                sales.payments(id, start, end, Optional.empty()),
                withGoal ? goal(id, window) : Optional.empty());
    }

    /**
     * A meta do mês do dia do resumo (REL-79): do começo do mês (no horário do resumo) até o fim do dia;
     * os dias que faltam são os depois dele.
     */
    private Optional<GoalProgress> goal(EstablishmentId id, SummaryWindow window) {
        LocalDate day = window.day();
        YearMonth month = YearMonth.from(day);
        return goals.find(id, month).map(goal -> {
            Instant monthStart = new SummaryWindow(month.atDay(1), window.hour(), window.zone()).start();
            Money achieved = sales.totals(id, monthStart, window.end(), Optional.empty()).revenue();
            return GoalAnalysis.progress(goal, achieved, month.lengthOfMonth() - day.getDayOfMonth(), achieved,
                    day.getDayOfMonth());
        });
    }
}
