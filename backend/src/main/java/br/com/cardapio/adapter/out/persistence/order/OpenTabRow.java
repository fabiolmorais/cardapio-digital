package br.com.cardapio.adapter.out.persistence.order;

/**
 * Uma comanda aberta com o resumo calculado no banco (SUM … GROUP BY): o total dos itens não
 * cancelados, em centavos, e quantos itens ainda estão a caminho.
 */
public record OpenTabRow(TabJpaEntity tab, String tableLabel, String sectorName, Long totalCents,
                         Long itemsOnTheWay) {
}
