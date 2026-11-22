package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.shared.model.Money;

/**
 * Quantas vezes uma opção ou adicional foi escolhido e quanto somou (REL-19).
 *
 * @param charged o que entrou de fato no preço (zero nos grátis e nas escolhas sem preço)
 * @param priced  a opção tem preço (mesmo que tenha saído de graça pela regra do grupo)
 */
public record OptionSales(String group, String option, int times, Money charged, boolean priced) {
}
