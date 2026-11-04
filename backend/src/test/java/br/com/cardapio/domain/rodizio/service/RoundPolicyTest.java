package br.com.cardapio.domain.rodizio.service;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.rodizio.exception.RoundNotAllowedException;
import br.com.cardapio.domain.rodizio.model.PriceTier;
import br.com.cardapio.domain.rodizio.model.Rodizio;
import br.com.cardapio.domain.rodizio.model.RodizioRules;
import br.com.cardapio.domain.rodizio.model.RodizioType;
import br.com.cardapio.domain.rodizio.model.TabRodizio;
import br.com.cardapio.domain.shared.model.Money;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoundPolicyTest {

    private static final Instant NOW = Instant.parse("2026-11-06T23:00:00Z");

    /** O rodízio com estas regras, ativado agora para 4 pessoas. */
    private static TabRodizio fourPeople(RodizioRules rules) {
        Rodizio rodizio = Rodizio.create(EstablishmentId.newId(), "Rodízio Japonês", RodizioType.A_LA_MINUTE, "",
                rules, List.of(new PriceTier("Adulto", Money.ofCents(129_90), List.of())), Set.of(), NOW);
        return TabRodizio.activate(rodizio, TabId.newId(), Map.of("Adulto", 4), LocalDateTime.of(2026, 11, 6, 20, 0),
                UserId.newId(), NOW);
    }

    private static RoundPolicy policy(RodizioRules rules, int undelivered, Instant lastRoundAt) {
        return new RoundPolicy(rules, fourPeople(rules), undelivered, lastRoundAt);
    }

    private static List<RoundPolicy.Line> round(int guiozas, int hotRolls) {
        return List.of(new RoundPolicy.Line("Guioza", guiozas), new RoundPolicy.Line("Hot roll", hotRolls));
    }

    @Test
    void withNoRulesTheTableOrdersAsMuchAsItWants() {
        RoundPolicy free = policy(RodizioRules.NONE, 50, NOW.minusSeconds(5));

        assertThatCode(() -> free.check(round(20, 20), NOW)).doesNotThrowAnyException();
        assertThat(free.limits(NOW)).isEqualTo(new RoundPolicy.Limits(null, null, null, null, null));
    }

    @Test
    void theLimitPerItemIsPerPerson() {
        RodizioRules twoEach = new RodizioRules(2, null, null, null, null);

        assertThatCode(() -> policy(twoEach, 0, null).check(round(8, 3), NOW)).doesNotThrowAnyException();
        // Duas linhas do mesmo produto somam.
        assertThatThrownBy(() -> policy(twoEach, 0, null).check(List.of(new RoundPolicy.Line("Guioza", 5),
                new RoundPolicy.Line("Guioza", 4)), NOW))
                .isInstanceOf(RoundNotAllowedException.class)
                .hasMessage("Nesta rodada, até 8 de Guioza (2 por pessoa).");
        assertThat(policy(twoEach, 0, null).limits(NOW).maxPerItem()).isEqualTo(8);
    }

    @Test
    void theUndeliveredItemsHoldTheNextRound() {
        RodizioRules fourEach = new RodizioRules(null, 4, null, null, null);

        assertThatCode(() -> policy(fourEach, 10, null).check(round(3, 3), NOW)).doesNotThrowAnyException();
        assertThatThrownBy(() -> policy(fourEach, 10, null).check(round(4, 3), NOW))
                .isInstanceOf(RoundNotAllowedException.class)
                .hasMessage("Com o que ainda está a caminho, cabem só mais 6 itens nesta rodada.");
        assertThatThrownBy(() -> policy(fourEach, 16, null).check(round(1, 0), NOW))
                .isInstanceOf(RoundNotAllowedException.class).hasMessageContaining("Ainda há 16 itens a caminho");
        assertThat(policy(fourEach, 10, null).limits(NOW).room()).isEqualTo(6);
    }

    @Test
    void theNextRoundWaitsForTheInterval() {
        RodizioRules tenMinutes = new RodizioRules(null, null, 10, null, null);
        Instant last = NOW.minusSeconds(4 * 60 + 30);

        assertThatThrownBy(() -> policy(tenMinutes, 0, last).check(round(1, 0), NOW))
                .isInstanceOf(RoundNotAllowedException.class).hasMessage("A próxima rodada libera em 6 minutos.");
        assertThat(policy(tenMinutes, 0, last).limits(NOW).nextRoundAt()).isEqualTo(last.plusSeconds(600));
        assertThatCode(() -> policy(tenMinutes, 0, last).check(round(1, 0), last.plusSeconds(600)))
                .doesNotThrowAnyException();
        assertThatCode(() -> policy(tenMinutes, 0, null).check(round(1, 0), NOW)).doesNotThrowAnyException();
    }

    @Test
    void theTimeLimitIsSetAtActivationAndEndsTheRounds() {
        RodizioRules twoHours = new RodizioRules(null, null, null, 120, "Sobras são cobradas à parte.");
        RoundPolicy policy = policy(twoHours, 0, null);

        assertThat(policy.rodizio().endsAt()).isEqualTo(NOW.plusSeconds(2 * 3600));
        assertThat(policy.limits(NOW).endsAt()).isEqualTo(NOW.plusSeconds(2 * 3600));
        assertThat(policy.limits(NOW).wasteNotice()).isEqualTo("Sobras são cobradas à parte.");
        assertThatCode(() -> policy.check(round(1, 0), NOW.plusSeconds(2 * 3600 - 1))).doesNotThrowAnyException();
        assertThatThrownBy(() -> policy.check(round(1, 0), NOW.plusSeconds(2 * 3600)))
                .isInstanceOf(RoundNotAllowedException.class).hasMessageContaining("O tempo do rodízio acabou");
    }
}
