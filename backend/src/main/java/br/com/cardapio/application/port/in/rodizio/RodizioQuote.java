package br.com.cardapio.application.port.in.rodizio;

import br.com.cardapio.domain.rodizio.model.Rodizio;
import br.com.cardapio.domain.rodizio.model.RodizioId;
import br.com.cardapio.domain.rodizio.model.RodizioType;
import br.com.cardapio.domain.shared.model.Money;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Um rodízio com o preço de cada faixa AGORA, no relógio do restaurante: é o que a mesa vê em
 * "Escolha o rodízio" e o que o garçom vê ao ativar.
 */
public record RodizioQuote(RodizioId id, String name, RodizioType type, String description, List<Tier> tiers) {

    public RodizioQuote {
        tiers = List.copyOf(tiers);
    }

    public record Tier(String name, Money price) {
    }

    public static RodizioQuote of(Rodizio rodizio, LocalDateTime localTime) {
        return new RodizioQuote(rodizio.id(), rodizio.name(), rodizio.type(), rodizio.description(),
                rodizio.tiers().stream()
                        .map(t -> new Tier(t.name(), t.priceAt(localTime.getDayOfWeek(), localTime.toLocalTime())))
                        .toList());
    }
}
