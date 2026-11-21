package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.call.model.PaymentMethod;
import br.com.cardapio.domain.shared.model.Money;

/** O que entrou por uma forma de pagamento (com a taxa de serviço: é o que vai para o caixa). */
public record PaymentTotal(PaymentMethod method, Money amount) {
}
