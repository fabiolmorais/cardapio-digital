package br.com.cardapio.domain.order.model;

/** A comanda da mesa. "Fechando" e "fechada" entram na Etapa 9 (pedir a conta e fechar). */
public enum TabStatus {
    OPEN,
    CLOSING,
    CLOSED;

    /** Aberta ou fechando: ainda é a comanda da mesa, e recebe pedidos. */
    public boolean isOpen() {
        return this != CLOSED;
    }
}
