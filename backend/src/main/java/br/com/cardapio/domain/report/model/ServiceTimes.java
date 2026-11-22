package br.com.cardapio.domain.report.model;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Os tempos de entrega de um período (REL-31, 32, 35).
 *
 * @param passe              de pronto até entregue, em média (itens entregues)
 * @param toTable            da chegada à cozinha até entregue, em média
 * @param lateConfirmations  pedidos que esperaram a confirmação do garçom mais que o limite
 */
public record ServiceTimes(Optional<Duration> passe, Optional<Duration> toTable, int lateConfirmations) {

    public ServiceTimes {
        Objects.requireNonNull(passe);
        Objects.requireNonNull(toTable);
    }
}
