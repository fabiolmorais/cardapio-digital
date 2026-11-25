package br.com.cardapio.application.port.in.unit;

/**
 * Quantas unidades ativas a empresa tem e quantas o plano permite.
 *
 * @param max nulo: sem limite (o Enterprise)
 */
public record UnitAllowance(String planName, long used, Integer max) {

    /** Ainda cabe mais uma unidade no plano? */
    public boolean canAddMore() {
        return max == null || used < max;
    }
}
