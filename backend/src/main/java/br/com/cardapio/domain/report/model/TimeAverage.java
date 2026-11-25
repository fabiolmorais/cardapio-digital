package br.com.cardapio.domain.report.model;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Uma média de tempo e quantas medidas a formaram (o peso para juntar médias de várias unidades).
 *
 * @param count   quantas medidas (preparos, chamados)
 * @param average a média; vazia sem medidas
 */
public record TimeAverage(int count, Optional<Duration> average) {

    public static final TimeAverage NONE = new TimeAverage(0, Optional.empty());

    public TimeAverage {
        Objects.requireNonNull(average, "média é obrigatória");
        if (count < 0) {
            throw new IllegalArgumentException("quantidade não pode ser negativa");
        }
        if (count == 0) {
            average = Optional.empty();
        }
    }

    /** A média das médias, pesada pela quantidade de cada uma; arredondada ao segundo. */
    public static TimeAverage combine(List<TimeAverage> parts) {
        long count = 0;
        long weighted = 0;
        for (TimeAverage part : parts) {
            if (part.average.isPresent()) {
                count += part.count;
                weighted += part.average.get().toMillis() * part.count;
            }
        }
        if (count == 0) {
            return NONE;
        }
        return new TimeAverage(Math.toIntExact(count),
                Optional.of(Duration.ofSeconds(Math.round(weighted / 1000.0 / count))));
    }
}
