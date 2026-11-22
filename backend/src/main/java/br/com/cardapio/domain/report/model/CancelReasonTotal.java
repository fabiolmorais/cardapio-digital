package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.shared.model.Money;

/**
 * O valor cancelado por um motivo (REL-12).
 *
 * @param reason o motivo; null junta os outros (os menos comuns e os sem motivo)
 */
public record CancelReasonTotal(String reason, Money amount) {
}
