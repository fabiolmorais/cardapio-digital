package br.com.cardapio.adapter.out.persistence.order;

/** A comanda com a mesa e o setor, numa consulta só. */
public record TabRow(TabJpaEntity tab, String tableLabel, String sectorName) {
}
