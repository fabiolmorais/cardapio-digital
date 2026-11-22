package br.com.cardapio.domain.report.service;

import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.report.model.HourShare;
import br.com.cardapio.domain.report.model.Occupancy;
import br.com.cardapio.domain.report.model.OccupancyPeak;
import br.com.cardapio.domain.report.model.OccupiedInterval;
import br.com.cardapio.domain.shared.model.Money;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/** As regras de mesas e ocupação (REL-57, 58, 60, 61). */
public final class OccupancyAnalysis {

    /** Uma comanda aberta há mais que isto está esquecida; na ocupação, ela conta só até este limite. */
    public static final Duration FORGOTTEN = Duration.ofHours(6);

    /** Quantas comandas esquecidas aparecem. */
    public static final int FORGOTTEN_SHOWN = 5;

    private OccupancyAnalysis() {
    }

    /** O giro: comandas fechadas por mesa por dia; vazio sem mesas. */
    public static Optional<Double> turnover(int tabs, int tables, int days) {
        return tables == 0 || days == 0 ? Optional.empty() : Optional.of((double) tabs / tables / days);
    }

    /** O faturamento por hora de mesa ocupada; vazio sem tempo ocupado. */
    public static Optional<Money> perOccupiedHour(Money revenue, Duration occupied) {
        if (occupied.isZero() || occupied.isNegative()) {
            return Optional.empty();
        }
        return Optional.of(Money.ofCents(Math.round(revenue.cents() * 3600.0 / occupied.toSeconds())));
    }

    /**
     * A ocupação por hora do dia (no fuso): em cada dia com movimento, a fração das mesas que tiveram
     * comanda aberta em algum momento da hora; depois, a média desses dias. O pico é a hora da semana
     * com a maior média (pelos dias daquele dia da semana com movimento). Cada comanda conta no máximo
     * {@link #FORGOTTEN}, para uma esquecida não ocupar a mesa por dias. Só contam as horas que começam
     * dentro de [{@code start}, {@code end}).
     */
    public static Occupancy occupancy(Collection<OccupiedInterval> intervals, int tables, ZoneId zone,
                                      Instant start, Instant end) {
        if (tables == 0) {
            return new Occupancy(List.of(), Optional.empty());
        }
        Map<LocalDate, Map<Integer, Set<DiningTableId>>> used = new HashMap<>();
        for (OccupiedInterval interval : intervals) {
            Instant to = min(interval.to(), interval.from().plus(FORGOTTEN));
            ZonedDateTime slot = interval.from().atZone(zone).truncatedTo(ChronoUnit.HOURS);
            while (slot.toInstant().isBefore(to)) {
                Instant at = slot.toInstant();
                if (at.isBefore(start) || !at.isBefore(end)) {
                    slot = slot.plusHours(1);
                    continue;
                }
                used.computeIfAbsent(slot.toLocalDate(), d -> new HashMap<>())
                        .computeIfAbsent(slot.getHour(), h -> new HashSet<>())
                        .add(interval.table());
                slot = slot.plusHours(1);
            }
        }
        if (used.isEmpty()) {
            return new Occupancy(List.of(), Optional.empty());
        }
        int days = used.size();
        Map<Integer, Double> byHour = new TreeMap<>();
        Map<DayOfWeek, Integer> weekdays = new HashMap<>();
        Map<DayOfWeek, Map<Integer, Double>> byWeekday = new HashMap<>();
        used.forEach((day, hours) -> {
            weekdays.merge(day.getDayOfWeek(), 1, Integer::sum);
            hours.forEach((hour, set) -> {
                double share = (double) set.size() / tables;
                byHour.merge(hour, share, Double::sum);
                byWeekday.computeIfAbsent(day.getDayOfWeek(), w -> new HashMap<>()).merge(hour, share, Double::sum);
            });
        });
        List<HourShare> hours = new ArrayList<>();
        byHour.forEach((hour, sum) -> hours.add(new HourShare(hour, sum / days)));
        OccupancyPeak peak = null;
        for (DayOfWeek weekday : DayOfWeek.values()) {
            Map<Integer, Double> sums = byWeekday.get(weekday);
            if (sums == null) {
                continue;
            }
            for (var entry : new TreeMap<>(sums).entrySet()) {
                double share = entry.getValue() / weekdays.get(weekday);
                if (peak == null || share > peak.share()) {
                    peak = new OccupancyPeak(weekday, entry.getKey(), share);
                }
            }
        }
        return new Occupancy(hours, Optional.ofNullable(peak));
    }

    private static Instant min(Instant a, Instant b) {
        return a.isBefore(b) ? a : b;
    }
}
