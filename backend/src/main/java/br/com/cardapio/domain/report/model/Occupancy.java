package br.com.cardapio.domain.report.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** A ocupação por hora do dia e o pico da semana; vazios sem comandas. */
public record Occupancy(List<HourShare> byHour, Optional<OccupancyPeak> peak) {

    public Occupancy {
        byHour = List.copyOf(byHour);
        Objects.requireNonNull(peak, "pico é obrigatório");
    }
}
