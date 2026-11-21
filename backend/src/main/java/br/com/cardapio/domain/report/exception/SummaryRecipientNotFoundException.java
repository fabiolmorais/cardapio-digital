package br.com.cardapio.domain.report.exception;

import br.com.cardapio.domain.shared.exception.NotFoundException;

/** Quem foi escolhido para receber o resumo não é dono nem gerente ativo deste restaurante. */
public class SummaryRecipientNotFoundException extends NotFoundException {

    public SummaryRecipientNotFoundException() {
        super("Pessoa não encontrada na equipe");
    }
}
