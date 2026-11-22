package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.floor.model.SectorId;
import br.com.cardapio.domain.shared.model.Money;

/**
 * O que um setor faturou no período (REL-06), com quantas mesas ativas ele tem.
 *
 * @param tables as mesas ativas do setor hoje
 */
public record SectorSales(SectorId sectorId, String name, int tables, Money revenue) {

    /** Por mesa, por dia: o faturamento dividido pelas mesas e pelos dias do período; zero sem mesas. */
    public Money perTablePerDay(int days) {
        if (tables == 0 || days <= 0) {
            return Money.ZERO;
        }
        return Money.ofCents(Math.round((double) revenue.cents() / tables / days));
    }
}
