package br.com.cardapio.domain.report.model;

/**
 * Dois produtos pedidos na mesma comanda (REL-21).
 *
 * @param firstTabs  as comandas em que o primeiro foi pedido
 * @param secondTabs as comandas em que o segundo foi pedido
 * @param together   as comandas em que os dois foram pedidos
 */
public record ProductPair(String first, String second, int firstTabs, int secondTabs, int together) {

    /**
     * Do jeito que se lê melhor ("das mesas que pediram o primeiro, quantas pediram o segundo"): o
     * primeiro é o menos pedido dos dois.
     */
    public ProductPair leading() {
        return secondTabs < firstTabs ? new ProductPair(second, first, secondTabs, firstTabs, together) : this;
    }

    /** Das comandas do primeiro, a parte que pediu também o segundo (0 a 1). */
    public double share() {
        return firstTabs == 0 ? 0 : (double) together / firstTabs;
    }
}
