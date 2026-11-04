package br.com.cardapio.domain.rodizio.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;

/**
 * As regras opcionais do rodízio. Todas vazias por padrão (especificação): o cliente pede o
 * quanto quiser. O restaurante liga as que quiser; elas passam a valer na mesa no Guia 26.
 *
 * @param maxPerItemPerRound      unidades de cada item numa rodada, por pessoa
 * @param maxPendingPerGuest      itens ainda não entregues, por pessoa, antes de uma rodada nova
 * @param minRoundIntervalMinutes o tempo mínimo entre uma rodada e a seguinte
 * @param timeLimitMinutes        a duração do rodízio, com aviso 15 minutos antes do fim
 * @param wasteNotice             o aviso de taxa de desperdício, no topo do cardápio
 */
public record RodizioRules(Integer maxPerItemPerRound, Integer maxPendingPerGuest, Integer minRoundIntervalMinutes,
                           Integer timeLimitMinutes, String wasteNotice) {

    /** Nenhuma regra ligada: o padrão. */
    public static final RodizioRules NONE = new RodizioRules(null, null, null, null, null);

    public RodizioRules {
        between(maxPerItemPerRound, 1, 50, "O máximo por item em cada rodada vai de 1 a 50.");
        between(maxPendingPerGuest, 1, 100, "O limite de itens não entregues por pessoa vai de 1 a 100.");
        between(minRoundIntervalMinutes, 1, 120, "O intervalo entre rodadas vai de 1 a 120 minutos.");
        between(timeLimitMinutes, 15, 600, "O tempo limite vai de 15 minutos a 10 horas.");
        wasteNotice = wasteNotice == null || wasteNotice.isBlank() ? null : wasteNotice.strip();
        if (wasteNotice != null && wasteNotice.length() > 200) {
            throw new InvalidValueException("O aviso de desperdício pode ter até 200 letras.");
        }
    }

    private static void between(Integer value, int min, int max, String message) {
        if (value != null && (value < min || value > max)) {
            throw new InvalidValueException(message);
        }
    }
}
