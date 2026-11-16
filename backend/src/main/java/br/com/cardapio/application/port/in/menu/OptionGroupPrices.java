package br.com.cardapio.application.port.in.menu;

import br.com.cardapio.domain.menu.model.OptionItemId;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.menu.model.ProductVariantId;
import br.com.cardapio.domain.shared.model.Money;

import java.util.List;
import java.util.Objects;

/**
 * A grade de preços de um grupo de opções (complemento da ADR 0010): o preço de cada opção em cada
 * tamanho dos produtos que usam o grupo, editado num lugar só, junto com o sabor.
 *
 * @param columns as colunas da grade: um tamanho de um produto, ou o produto sem tamanhos (a
 *                Pequena, a Média e a Grande cadastradas como produtos)
 * @param prices  os preços preenchidos; o que fica em branco vale o preço da opção
 */
public record OptionGroupPrices(List<Column> columns, List<Price> prices) {

    public OptionGroupPrices {
        columns = List.copyOf(columns);
        prices = List.copyOf(prices);
    }

    /** @param variantId null quando a coluna é o produto todo (sem tamanhos) */
    public record Column(ProductId productId, String productName, ProductVariantId variantId, String variantName) {

        public Column {
            Objects.requireNonNull(productId);
        }
    }

    /** O preço de uma opção numa coluna. @param variantId null no produto sem tamanhos */
    public record Price(ProductId productId, ProductVariantId variantId, OptionItemId optionId, Money price) {

        public Price {
            Objects.requireNonNull(productId);
            Objects.requireNonNull(optionId);
            Objects.requireNonNull(price);
        }
    }
}
