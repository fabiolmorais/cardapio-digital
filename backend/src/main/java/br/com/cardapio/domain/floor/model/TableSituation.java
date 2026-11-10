package br.com.cardapio.domain.floor.model;

/**
 * Como a mesa está agora, para a lista de mesas do painel (desenho 12.1). Vale o que pede mais
 * atenção: a desativada não recebe ninguém; a que pediu a conta vem antes do rodízio, e o rodízio
 * antes da comanda aberta comum.
 */
public enum TableSituation {
    FREE,
    OCCUPIED,
    RODIZIO,
    BILL_REQUESTED,
    INACTIVE;

    public static TableSituation of(boolean tableActive, boolean tabOpen, boolean billRequested, boolean rodizio) {
        if (!tableActive) {
            return INACTIVE;
        }
        if (billRequested) {
            return BILL_REQUESTED;
        }
        if (rodizio) {
            return RODIZIO;
        }
        return tabOpen ? OCCUPIED : FREE;
    }
}
