package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.shared.model.Money;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * O rodízio das comandas fechadas no período (REL-70, 71, 73, 75).
 *
 * @param tabs         as comandas com rodízio
 * @param people       as pessoas das faixas
 * @param revenue      o valor do rodízio (pessoas × preço de cada faixa)
 * @param extra        o "à parte": o resto dessas comandas (bebidas, sobremesas)
 * @param rounds       as rodadas (pedidos com item do rodízio, sem os recusados)
 * @param items        os itens do rodízio pedidos (sem os cancelados)
 * @param time         o tempo médio, do início do rodízio ao fechamento; vazio sem comandas
 * @param withLimit    as comandas com tempo limite
 * @param reachedLimit dessas, as que fecharam depois do limite
 */
public record RodizioTotals(int tabs, int people, Money revenue, Money extra, int rounds, int items,
                            Optional<Duration> time, int withLimit, int reachedLimit) {

    public RodizioTotals {
        Objects.requireNonNull(revenue, "faturamento é obrigatório");
        Objects.requireNonNull(extra, "à parte é obrigatório");
        Objects.requireNonNull(time, "tempo é obrigatório");
    }
}
