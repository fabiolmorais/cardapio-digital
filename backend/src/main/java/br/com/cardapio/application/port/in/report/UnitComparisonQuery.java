package br.com.cardapio.application.port.in.report;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;

import java.time.LocalDate;
import java.util.Objects;

/**
 * O pedido da comparação entre unidades: a unidade de agora e a pessoa (as duas do token), e os dias.
 * As outras unidades saem da empresa da unidade de agora, nunca de fora.
 */
public record UnitComparisonQuery(EstablishmentId establishmentId, UserId userId, LocalDate from, LocalDate to) {

    public UnitComparisonQuery {
        Objects.requireNonNull(establishmentId, "restaurante é obrigatório");
        Objects.requireNonNull(userId, "usuário é obrigatório");
        Objects.requireNonNull(from, "início é obrigatório");
        Objects.requireNonNull(to, "fim é obrigatório");
    }
}
