package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.shared.model.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Os números de operação de um período (os completos da Visão geral, REL-32, 63, 38, 56, 12).
 *
 * @param toTable        do pedido chegar à cozinha até a mesa, em média (itens entregues)
 * @param phoneOrders    pedidos feitos pelo celular da mesa
 * @param staffOrders    pedidos lançados pela equipe
 * @param callResponse   do chamado até o garçom aceitar, em média
 * @param stay           da comanda abrir até fechar, em média (comandas fechadas)
 * @param canceled       o valor dos itens cancelados nas comandas fechadas
 */
public record OperationTotals(Optional<Duration> toTable, int phoneOrders, int staffOrders,
                              Optional<Duration> callResponse, Optional<Duration> stay, Money canceled) {

    public OperationTotals {
        Objects.requireNonNull(toTable);
        Objects.requireNonNull(callResponse);
        Objects.requireNonNull(stay);
        Objects.requireNonNull(canceled);
    }

    /** A parte dos pedidos feita pelo celular, de 0 a 100, com uma casa; vazio sem pedidos. */
    public Optional<BigDecimal> phoneShare() {
        int all = phoneOrders + staffOrders;
        if (all == 0) {
            return Optional.empty();
        }
        return Optional.of(BigDecimal.valueOf(phoneOrders * 100L)
                .divide(BigDecimal.valueOf(all), 1, RoundingMode.HALF_UP));
    }
}
