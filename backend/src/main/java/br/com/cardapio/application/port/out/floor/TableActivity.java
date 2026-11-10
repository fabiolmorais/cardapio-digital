package br.com.cardapio.application.port.out.floor;

/**
 * O movimento de uma mesa agora: comanda aberta, conta pedida (o chamado de conta em aberto ou a
 * comanda já em fechamento) e rodízio ativo na comanda.
 */
public record TableActivity(boolean tabOpen, boolean billRequested, boolean rodizio) {

    public static final TableActivity NONE = new TableActivity(false, false, false);
}
