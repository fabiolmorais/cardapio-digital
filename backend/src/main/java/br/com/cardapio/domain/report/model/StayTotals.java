package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.shared.model.Money;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * As comandas fechadas do período e o tempo que ocuparam a mesa (REL-56, 57, 60).
 *
 * @param tabs          quantas comandas fecharam
 * @param revenue       o faturamento delas
 * @param occupied      a soma do tempo de cada uma, da abertura ao fechamento
 * @param stay          a permanência média; vazio sem comandas
 * @param rodizioStay   a permanência média das comandas com rodízio; vazio sem elas
 * @param aLaCarteStay  a das comandas sem rodízio; vazio sem elas
 */
public record StayTotals(int tabs, Money revenue, Duration occupied, Optional<Duration> stay,
                         Optional<Duration> rodizioStay, Optional<Duration> aLaCarteStay) {

    public StayTotals {
        Objects.requireNonNull(revenue, "faturamento é obrigatório");
        Objects.requireNonNull(occupied, "tempo ocupado é obrigatório");
        Objects.requireNonNull(stay, "permanência é obrigatória");
        Objects.requireNonNull(rodizioStay, "permanência do rodízio é obrigatória");
        Objects.requireNonNull(aLaCarteStay, "permanência à la carte é obrigatória");
    }
}
