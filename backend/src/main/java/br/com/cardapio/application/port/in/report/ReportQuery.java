package br.com.cardapio.application.port.in.report;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.SectorId;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * O pedido de um relatório: o restaurante (sempre do usuário logado), os dias e, se escolhido, o setor.
 *
 * @param sector o setor das mesas; null = todos
 */
public record ReportQuery(EstablishmentId establishmentId, LocalDate from, LocalDate to, SectorId sector) {

    public ReportQuery {
        Objects.requireNonNull(establishmentId, "restaurante é obrigatório");
    }

    public Optional<SectorId> sectorFilter() {
        return Optional.ofNullable(sector);
    }
}
