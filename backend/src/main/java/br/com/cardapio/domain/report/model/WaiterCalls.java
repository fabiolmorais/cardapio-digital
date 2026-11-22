package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.account.model.UserId;

import java.time.Duration;

/** Um garçom no período: quantos chamados aceitou e em quanto tempo, em média (REL-46, 48). */
public record WaiterCalls(UserId userId, String name, int calls, Duration averageResponse) {
}
