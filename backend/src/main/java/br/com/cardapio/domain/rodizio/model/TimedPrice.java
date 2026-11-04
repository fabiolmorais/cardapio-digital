package br.com.cardapio.domain.rodizio.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.shared.model.Money;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Uma exceção do preço de uma faixa: nestes dias, das startsAt às endsAt (no relógio do
 * restaurante), a faixa custa price ("jantar de sexta e sábado, das 18h às 23h59").
 */
public record TimedPrice(Set<DayOfWeek> days, LocalTime startsAt, LocalTime endsAt, Money price) {

    public TimedPrice {
        if (days == null || days.isEmpty()) {
            throw new InvalidValueException("Escolha pelo menos um dia para o preço especial.");
        }
        days = Set.copyOf(EnumSet.copyOf(days));
        Objects.requireNonNull(startsAt);
        Objects.requireNonNull(endsAt);
        Objects.requireNonNull(price);
        if (!startsAt.isBefore(endsAt)) {
            throw new InvalidValueException("O preço especial precisa começar antes de terminar, no mesmo dia.");
        }
    }

    /** Vale neste dia e neste horário? O início entra; o fim, não. */
    public boolean appliesAt(DayOfWeek day, LocalTime time) {
        return days.contains(day) && !time.isBefore(startsAt) && time.isBefore(endsAt);
    }

    /** As duas exceções valeriam ao mesmo tempo em algum momento? */
    boolean overlaps(TimedPrice other) {
        return days.stream().anyMatch(other.days::contains)
                && startsAt.isBefore(other.endsAt) && other.startsAt.isBefore(endsAt);
    }
}
