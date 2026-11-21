package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.account.model.Role;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.shared.model.Email;

import java.util.Optional;

/**
 * Quem pode receber o resumo diário: o dono e os gerentes ativos do restaurante.
 *
 * @param email    vazio para quem só entra pelo PIN (o resumo vai só no celular)
 * @param receives se escolheu receber (começa ligado)
 */
public record SummaryRecipient(UserId userId, String name, Role role, Optional<Email> email, boolean receives) {
}
