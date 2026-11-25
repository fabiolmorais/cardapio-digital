package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.account.model.EstablishmentId;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;

/**
 * Onde o resumo diário de um restaurante parou.
 *
 * @param summarizedUntil o dia seguinte ao último resumido; vazio sem resumo
 * @param firstActivityDay o primeiro dia com comanda fechada ou item pedido; vazio sem movimento, ou
 *                         quando já há resumo (aí não é preciso)
 */
public record SummaryStatus(EstablishmentId establishmentId, ZoneId zone, Optional<LocalDate> summarizedUntil,
                            Optional<LocalDate> firstActivityDay) {

    public SummaryStatus {
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(zone);
        Objects.requireNonNull(summarizedUntil);
        Objects.requireNonNull(firstActivityDay);
    }
}
