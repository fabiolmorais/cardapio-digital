package br.com.cardapio.domain.order.model;

/**
 * A comanda da mesa. "Fechando" fica reservado: pedir a conta não trava a mesa (ela ainda pode
 * pedir a sobremesa), então a comanda continua aberta até o garçom fechar.
 */
public enum TabStatus {
    OPEN,
    CLOSING,
    CLOSED,
    /** Juntada a outra comanda (ADR 0012): os pedidos e os pagamentos foram para ela. Não tem total. */
    MERGED;

    /** Aberta ou fechando: ainda é a comanda da mesa, e recebe pedidos. */
    public boolean isOpen() {
        return this == OPEN || this == CLOSING;
    }
}
