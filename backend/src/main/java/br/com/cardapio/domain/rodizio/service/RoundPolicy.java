package br.com.cardapio.domain.rodizio.service;

import br.com.cardapio.domain.rodizio.exception.RoundNotAllowedException;
import br.com.cardapio.domain.rodizio.model.RodizioRules;
import br.com.cardapio.domain.rodizio.model.TabRodizio;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * As regras opcionais do rodízio, aplicadas a uma rodada (especificação). Por padrão nenhuma vale:
 * o cliente pede o quanto quiser. Os limites "por pessoa" valem vezes as pessoas da mesa: com "2
 * por pessoa" e 4 pessoas, a rodada leva até 8 de cada item.
 *
 * @param rules       as regras do cadastro, como estão agora
 * @param rodizio     o rodízio da comanda (as pessoas e o fim previsto)
 * @param undelivered os itens do rodízio da mesa ainda a caminho (na fila, em preparo ou prontos)
 * @param lastRoundAt quando a última rodada (não recusada) foi pedida; null na primeira
 */
public record RoundPolicy(RodizioRules rules, TabRodizio rodizio, int undelivered, Instant lastRoundAt) {

    /** Uma linha da rodada: o nome (para a mensagem) e a quantidade. */
    public record Line(String productName, int quantity) {
    }

    /**
     * O que a tela mostra para a mesa não esbarrar nas regras. Cada campo é null quando a regra
     * está desligada.
     *
     * @param maxPerItem  unidades de cada item numa rodada, para a mesa toda
     * @param room        quantos itens ainda cabem agora, pelo limite de não entregues
     * @param nextRoundAt quando a próxima rodada libera, se o intervalo ainda não passou
     */
    public record Limits(Integer maxPerItem, Integer room, Instant nextRoundAt, Instant endsAt, String wasteNotice) {
    }

    public Limits limits(Instant now) {
        Integer maxPending = perTable(rules.maxPendingPerGuest());
        Instant next = nextRoundAt();
        return new Limits(perTable(rules.maxPerItemPerRound()),
                maxPending == null ? null : Math.max(0, maxPending - undelivered),
                next != null && next.isAfter(now) ? next : null, rodizio.endsAt(), rules.wasteNotice());
    }

    /** Confere a rodada inteira. A primeira regra que ela fere vira a mensagem do 422. */
    public void check(List<Line> round, Instant now) {
        if (rodizio.endsAt() != null && !now.isBefore(rodizio.endsAt())) {
            throw new RoundNotAllowedException(
                    "O tempo do rodízio acabou. Bebidas e sobremesas continuam pelo carrinho.");
        }
        Instant next = nextRoundAt();
        if (next != null && now.isBefore(next)) {
            long minutes = Math.max(1, Duration.between(now, next).plusSeconds(59).toMinutes());
            throw new RoundNotAllowedException("A próxima rodada libera em " + minutes
                    + (minutes == 1 ? " minuto." : " minutos."));
        }
        Integer maxPerItem = perTable(rules.maxPerItemPerRound());
        if (maxPerItem != null) {
            Map<String, Integer> byProduct = round.stream()
                    .collect(Collectors.toMap(Line::productName, Line::quantity, Integer::sum));
            byProduct.forEach((name, quantity) -> {
                if (quantity > maxPerItem) {
                    throw new RoundNotAllowedException("Nesta rodada, até " + maxPerItem + " de " + name + " ("
                            + rules.maxPerItemPerRound() + " por pessoa).");
                }
            });
        }
        Integer maxPending = perTable(rules.maxPendingPerGuest());
        if (maxPending != null) {
            int total = round.stream().mapToInt(Line::quantity).sum();
            int room = maxPending - undelivered;
            if (room <= 0) {
                throw new RoundNotAllowedException("Ainda há " + undelivered
                        + (undelivered == 1 ? " item" : " itens")
                        + " a caminho. A próxima rodada libera quando parte deles chegar.");
            }
            if (total > room) {
                throw new RoundNotAllowedException("Com o que ainda está a caminho, cabem só mais " + room
                        + (room == 1 ? " item" : " itens") + " nesta rodada.");
            }
        }
    }

    private Instant nextRoundAt() {
        Integer interval = rules.minRoundIntervalMinutes();
        return interval == null || lastRoundAt == null ? null : lastRoundAt.plus(Duration.ofMinutes(interval));
    }

    private Integer perTable(Integer perGuest) {
        return perGuest == null ? null : perGuest * rodizio.people();
    }
}
