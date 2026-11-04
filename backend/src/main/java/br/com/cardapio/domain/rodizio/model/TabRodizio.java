package br.com.cardapio.domain.rodizio.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.rodizio.exception.RodizioNotServedException;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.shared.model.Money;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * O rodízio de uma comanda: qual, quantas pessoas por faixa (com o preço copiado) e quem ativou.
 * Quem ativa é a equipe, porque o número de pessoas define a conta (especificação).
 *
 * @param rodizioName o nome copiado na ativação
 * @param guests      todas as faixas do rodízio, na ordem do cadastro, mesmo as com 0 pessoas: assim
 *                    o garçom pode pôr uma criança depois, e o preço dela já está copiado
 */
public record TabRodizio(
        TabRodizioId id,
        EstablishmentId establishmentId,
        TabId tabId,
        RodizioId rodizioId,
        String rodizioName,
        List<GuestTier> guests,
        UserId activatedBy,
        Instant startedAt
) {

    public TabRodizio {
        Objects.requireNonNull(id);
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(tabId);
        Objects.requireNonNull(rodizioId);
        Objects.requireNonNull(rodizioName);
        Objects.requireNonNull(activatedBy);
        Objects.requireNonNull(startedAt);
        guests = List.copyOf(guests);
        if (guests.stream().mapToInt(GuestTier::quantity).sum() < 1) {
            throw new InvalidValueException("O rodízio precisa de pelo menos 1 pessoa.");
        }
    }

    /**
     * Ativa o rodízio na comanda, com o preço de cada faixa no dia e horário do restaurante.
     *
     * @param counts    pessoas por faixa, pelo nome da faixa; faixa que não vem fica com 0
     * @param localTime agora, no relógio do restaurante (para escolher o preço especial)
     */
    public static TabRodizio activate(Rodizio rodizio, TabId tabId, Map<String, Integer> counts,
                                      LocalDateTime localTime, UserId by, Instant now) {
        if (!rodizio.active()) {
            throw new InvalidValueException("Este rodízio foi desativado. Atualize a tela.");
        }
        if (rodizio.type() != RodizioType.A_LA_MINUTE) {
            throw new RodizioNotServedException();
        }
        requireKnownTiers(rodizio.tiers().stream().map(PriceTier::name).toList(), counts);
        List<GuestTier> guests = rodizio.tiers().stream()
                .map(t -> new GuestTier(t.name(), counts.getOrDefault(t.name(), 0),
                        t.priceAt(localTime.getDayOfWeek(), localTime.toLocalTime())))
                .toList();
        return new TabRodizio(TabRodizioId.newId(), rodizio.establishmentId(), tabId, rodizio.id(), rodizio.name(),
                guests, by, now);
    }

    /** Chegou ou saiu alguém: muda as quantidades. Os preços continuam os copiados na ativação. */
    public TabRodizio changeGuests(Map<String, Integer> counts) {
        requireKnownTiers(guests.stream().map(GuestTier::tierName).toList(), counts);
        return new TabRodizio(id, establishmentId, tabId, rodizioId, rodizioName,
                guests.stream().map(g -> g.withQuantity(counts.getOrDefault(g.tierName(), 0))).toList(),
                activatedBy, startedAt);
    }

    /** Quantas pessoas estão no rodízio, somando as faixas. */
    public int people() {
        return guests.stream().mapToInt(GuestTier::quantity).sum();
    }

    /** O que o rodízio soma na conta: pessoas × preço, faixa a faixa. */
    public Money total() {
        return guests.stream().map(GuestTier::subtotal).reduce(Money.ZERO, Money::plus);
    }

    private static void requireKnownTiers(List<String> tiers, Map<String, Integer> counts) {
        if (!new HashSet<>(tiers).containsAll(counts.keySet())) {
            throw new InvalidValueException("Alguma faixa de preço mudou no cadastro. Atualize a tela.");
        }
    }
}
