package br.com.cardapio.application.port.in.report;

import br.com.cardapio.domain.report.model.AttentionPoint;
import br.com.cardapio.domain.report.model.OptionSales;
import br.com.cardapio.domain.report.model.ProductPair;
import br.com.cardapio.domain.report.model.VariantSales;
import br.com.cardapio.domain.report.model.VideoComparison;
import br.com.cardapio.domain.shared.model.Money;

import java.util.List;

/**
 * Os blocos completos do Cardápio (Business, REL-18, 19, 21, 23, 26).
 *
 * @param sizes       os produtos com tamanhos, cada um com os tamanhos do mais escolhido para o menos
 * @param paidOptions o que os adicionais pagos somaram
 */
public record MenuFull(List<AttentionPoint.CanceledProduct> mostCanceled, List<List<VariantSales>> sizes,
                       List<OptionSales> options, Money paidOptions, List<ProductPair> pairs,
                       VideoComparison video) {
}
