package br.com.cardapio.application.port.out.report;

import br.com.cardapio.domain.account.model.EstablishmentId;

import java.time.Instant;
import java.util.Optional;

/** De quando são os dados que os relatórios têm para mostrar. */
public interface ReportActivityPort {

    /** Quando o restaurante fechou a primeira comanda; vazio se nunca fechou. */
    Optional<Instant> firstClosedTabAt(EstablishmentId establishmentId);
}
