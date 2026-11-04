package br.com.cardapio.domain.rodizio.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.shared.model.Money;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

/**
 * Uma faixa de preço do rodízio (Adulto, Criança 5 a 10…). O preço padrão vale sempre; as
 * exceções valem nos dias e horários delas. Duas exceções da mesma faixa não podem valer ao
 * mesmo tempo: o garçom não saberia qual preço cobrar.
 */
public record PriceTier(String name, Money defaultPrice, List<TimedPrice> exceptions) {

    public static final int MAX_EXCEPTIONS = 8;

    public PriceTier {
        name = name == null ? "" : name.strip();
        if (name.isEmpty() || name.length() > 40) {
            throw new InvalidValueException("Cada faixa precisa de um nome de até 40 letras (ex.: Adulto).");
        }
        Objects.requireNonNull(defaultPrice);
        exceptions = List.copyOf(exceptions);
        if (exceptions.size() > MAX_EXCEPTIONS) {
            throw new InvalidValueException("Cada faixa pode ter até " + MAX_EXCEPTIONS + " preços especiais.");
        }
        for (int i = 0; i < exceptions.size(); i++) {
            for (int j = i + 1; j < exceptions.size(); j++) {
                if (exceptions.get(i).overlaps(exceptions.get(j))) {
                    throw new InvalidValueException(
                            "Dois preços especiais de \"" + name + "\" valem ao mesmo tempo. Ajuste os dias ou os horários.");
                }
            }
        }
    }

    /** O preço da faixa neste dia e horário (do restaurante): a exceção que vale, ou o padrão. */
    public Money priceAt(DayOfWeek day, LocalTime time) {
        return exceptions.stream().filter(e -> e.appliesAt(day, time)).findFirst()
                .map(TimedPrice::price).orElse(defaultPrice);
    }
}
