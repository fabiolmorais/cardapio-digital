package br.com.cardapio.domain.report.model;

import java.time.DayOfWeek;
import java.util.Objects;

/** A hora da semana com a maior ocupação média (REL-58): "89%, sábado, 21h". */
public record OccupancyPeak(DayOfWeek weekday, int hour, double share) {

    public OccupancyPeak {
        Objects.requireNonNull(weekday, "dia da semana é obrigatório");
    }
}
