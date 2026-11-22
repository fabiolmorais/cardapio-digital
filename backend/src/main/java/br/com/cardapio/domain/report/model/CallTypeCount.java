package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.call.model.CallType;

/** Quantos chamados de um tipo, e quantos o cliente cancelou (REL-41, 42). */
public record CallTypeCount(CallType type, int calls, int canceled) {
}
