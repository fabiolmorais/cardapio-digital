package br.com.cardapio.domain.report.service;

import br.com.cardapio.domain.report.model.RatingDay;
import br.com.cardapio.domain.report.model.RatingPoint;
import br.com.cardapio.domain.report.model.ReportPeriod;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** As regras das avaliações nos relatórios (REL-55, 83, 84). */
public final class RatingAnalysis {

    /** Com menos avaliações que isto, a nota do garçom aparece como "—": poucas notas não dizem muito. */
    public static final int MIN_FOR_AVERAGE = 5;

    /** O almoço vai até esta hora (no fuso do restaurante); dali em diante, o jantar. */
    public static final int LUNCH_UNTIL = 16;

    /** Acima de tantos dias, a evolução da nota vai por semana; até eles, por dia. */
    public static final int DAILY_UP_TO = 14;

    private RatingAnalysis() {
    }

    /** A nota do garçom na tabela: a média com uma casa, ou null com poucas avaliações. */
    public static Double shownAverage(int count, long starSum) {
        return count < MIN_FOR_AVERAGE ? null : Math.round(starSum * 10.0 / count) / 10.0;
    }

    /**
     * A evolução da nota no período: um ponto por dia (até {@link #DAILY_UP_TO} dias) ou por semana,
     * contando do primeiro dia; dias e semanas sem avaliação entram com a média vazia.
     */
    public static List<RatingPoint> trend(ReportPeriod period, List<RatingDay> days) {
        Map<LocalDate, RatingDay> byDay = days.stream().collect(Collectors.toMap(RatingDay::day, Function.identity()));
        int step = period.days() > DAILY_UP_TO ? 7 : 1;
        List<RatingPoint> points = new ArrayList<>();
        for (LocalDate from = period.from(); !from.isAfter(period.to()); from = from.plusDays(step)) {
            LocalDate to = from.plusDays(step - 1L).isAfter(period.to()) ? period.to() : from.plusDays(step - 1L);
            int count = 0;
            long sum = 0;
            for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
                RatingDay day = byDay.get(d);
                if (day != null) {
                    count += day.count();
                    sum += day.starSum();
                }
            }
            points.add(new RatingPoint(from, to, count, count == 0 ? null : Math.round(sum * 10.0 / count) / 10.0));
        }
        return points;
    }
}
