package br.com.cardapio.application.port.out.plan;

import br.com.cardapio.domain.plan.model.Subscription;
import br.com.cardapio.domain.shared.model.Email;

/**
 * Para quem vai o aviso do teste: o e-mail da empresa (o do cadastro), o nome e o fuso do
 * restaurante (as datas do e-mail são as dele).
 */
public record TrialRecipient(Subscription subscription, Email email, String establishmentName, String timeZone) {
}
