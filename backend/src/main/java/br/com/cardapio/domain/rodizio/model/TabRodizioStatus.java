package br.com.cardapio.domain.rodizio.model;

/** O cartão digital da mesa no rodízio de passagem (especificação). */
public enum TabRodizioStatus {
    /** Cartão verde: os passadores podem servir a mesa. */
    SERVING,
    /** Cartão vermelho: a mesa pausou, e os passadores não servem até ela voltar. */
    PAUSED
}
