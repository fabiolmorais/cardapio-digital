package br.com.cardapio.domain.report.model;

/** Os alertas em tempo real (REL-78). */
public enum AlertType {
    /** Itens na cozinha há mais que o limite. */
    KITCHEN,
    /** Mesa chamando sem ninguém aceitar há mais que o limite. */
    CALL,
    /** Comanda aberta há mais que o limite. */
    TAB
}
