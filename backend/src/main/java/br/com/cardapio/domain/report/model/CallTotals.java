package br.com.cardapio.domain.report.model;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Os chamados de um período (REL-38, 39, 43).
 *
 * @param response      do chamado até o garçom aceitar, em média
 * @param done          do chamado até concluir, em média
 * @param billToClose   de pedir a conta até a comanda fechar, em média
 */
public record CallTotals(int calls, Optional<Duration> response, Optional<Duration> done,
                         Optional<Duration> billToClose) {

    public CallTotals {
        Objects.requireNonNull(response);
        Objects.requireNonNull(done);
        Objects.requireNonNull(billToClose);
    }
}
