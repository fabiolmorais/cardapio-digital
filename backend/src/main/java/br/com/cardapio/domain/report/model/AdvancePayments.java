package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.shared.model.Money;

/** As mesas que pagaram antes do fechamento (REL-11): quantas e quanto. */
public record AdvancePayments(int tabs, Money amount) {
}
