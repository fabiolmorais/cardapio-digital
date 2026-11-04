package br.com.cardapio.domain.rodizio.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.shared.model.Money;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PriceTierTest {

    private static TimedPrice dinner(long cents) {
        return new TimedPrice(Set.of(FRIDAY, SATURDAY), LocalTime.of(18, 0), LocalTime.of(23, 59), Money.ofCents(cents));
    }

    @Test
    void theExceptionWinsInsideItsDaysAndHoursAndTheDefaultElsewhere() {
        PriceTier adulto = new PriceTier(" Adulto ", Money.ofCents(12990), List.of(dinner(14990)));

        assertThat(adulto.name()).isEqualTo("Adulto");
        assertThat(adulto.priceAt(FRIDAY, LocalTime.of(18, 0))).isEqualTo(Money.ofCents(14990));
        assertThat(adulto.priceAt(SATURDAY, LocalTime.of(23, 58))).isEqualTo(Money.ofCents(14990));
        assertThat(adulto.priceAt(FRIDAY, LocalTime.of(17, 59))).isEqualTo(Money.ofCents(12990));
        assertThat(adulto.priceAt(FRIDAY, LocalTime.of(23, 59))).isEqualTo(Money.ofCents(12990)); // o fim não entra
        assertThat(adulto.priceAt(MONDAY, LocalTime.of(20, 0))).isEqualTo(Money.ofCents(12990));
    }

    @Test
    void twoExceptionsCannotApplyAtTheSameTime() {
        TimedPrice sundayLunch = new TimedPrice(Set.of(SUNDAY), LocalTime.of(11, 0), LocalTime.of(16, 0),
                Money.ofCents(15990));
        TimedPrice saturdayLate = new TimedPrice(Set.of(SATURDAY), LocalTime.of(22, 0), LocalTime.of(23, 0),
                Money.ofCents(9990));
        TimedPrice fridayLunch = new TimedPrice(Set.of(FRIDAY), LocalTime.of(11, 0), LocalTime.of(18, 0),
                Money.ofCents(9990));

        // Domingo não cruza com sexta e sábado; o almoço de sexta acaba às 18h, quando o jantar começa.
        assertThat(new PriceTier("Adulto", Money.ofCents(12990), List.of(dinner(14990), sundayLunch, fridayLunch))
                .exceptions()).hasSize(3);
        assertThatThrownBy(() -> new PriceTier("Adulto", Money.ofCents(12990), List.of(dinner(14990), saturdayLate)))
                .isInstanceOf(InvalidValueException.class)
                .hasMessageContaining("valem ao mesmo tempo");
    }

    @Test
    void anExceptionNeedsDaysAndStartsBeforeItEnds() {
        assertThatThrownBy(() -> new TimedPrice(Set.of(), LocalTime.NOON, LocalTime.MIDNIGHT.minusMinutes(1),
                Money.ofCents(1))).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new TimedPrice(Set.of(FRIDAY), LocalTime.of(23, 0), LocalTime.of(2, 0),
                Money.ofCents(1))).isInstanceOf(InvalidValueException.class).hasMessageContaining("no mesmo dia");
    }

    @Test
    void needsANameAndAtMostEightExceptions() {
        assertThatThrownBy(() -> new PriceTier(" ", Money.ofCents(1), List.of()))
                .isInstanceOf(InvalidValueException.class);
        List<TimedPrice> nine = java.util.stream.IntStream.range(0, 9)
                .mapToObj(h -> new TimedPrice(Set.of(MONDAY), LocalTime.of(h, 0), LocalTime.of(h, 30),
                        Money.ofCents(1)))
                .toList();
        assertThatThrownBy(() -> new PriceTier("Adulto", Money.ofCents(1), nine))
                .isInstanceOf(InvalidValueException.class).hasMessageContaining("até 8");
    }
}
