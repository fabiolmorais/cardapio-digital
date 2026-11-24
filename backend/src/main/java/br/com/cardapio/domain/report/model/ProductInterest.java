package br.com.cardapio.domain.report.model;

/**
 * Visto x pedido de um produto: quantas sessões viram e quantas dessas pediram.
 */
public record ProductInterest(String name, int viewers, int ordered) {

    /** De quem viu, a fração que pediu (0 a 1). */
    public double conversion() {
        return viewers == 0 ? 0 : (double) ordered / viewers;
    }
}
