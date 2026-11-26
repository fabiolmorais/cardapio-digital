package br.com.cardapio.domain.floor.model;

/**
 * O tipo do lugar do salão (ADR 0015): a mesa, com QR Code e uma comanda aberta por vez, ou o
 * balcão, sem QR Code, com várias comandas abertas (uma por cliente).
 */
public enum TableKind {
    TABLE,
    COUNTER;

    public boolean isCounter() {
        return this == COUNTER;
    }
}
