package br.com.cardapio.domain.device.model;

/** Para que o aparelho serve no restaurante. */
public enum DeviceType {
    /** Celular do salão: chamados, pedidos e comandas. */
    WAITER,
    /** Tablet da cozinha ou do bar: fila de pedidos. */
    KITCHEN,
    /** TV do Modo TV: só mostra, ninguém entra com PIN nela. */
    TV
}
